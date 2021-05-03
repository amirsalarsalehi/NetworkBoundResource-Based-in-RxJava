public abstract class NetworkBoundResource<Result, Request> {

    private static final String TAG = "NetworkBoundResource";

    private AppExecutors appExecutors;
    private MediatorLiveData<Resource<Result>> results = new MediatorLiveData<>();

    public NetworkBoundResourceSimple(AppExecutors appExecutors) {
        this.appExecutors = appExecutors;
        init();
    }

    private void init() {

        results.setValue(Resource.loading(null));

        final LiveData<Result> dbSource = LiveDataReactiveStreams.fromPublisher(loadFromDb());

        results.addSource(dbSource, result -> {

            results.removeSource(dbSource);

            if (shouldFetch(result)) {
                fetchFromNetwork(dbSource);
            } else {
                results.addSource(dbSource, result1 -> setValue(Resource.success(result1)));
            }

        });
    }

    private void fetchFromNetwork(final LiveData<Result> dbSource) {

        Log.d(TAG, "fetchFromNetwork: called.");

        results.addSource(dbSource, result -> setValue(Resource.loading(result)));

        final LiveData<ApiResponse<Request>> apiResponse = LiveDataReactiveStreams.fromPublisher(
                createCall()
                        .subscribeOn(Schedulers.io())
                        .observeOn(Schedulers.io())
                        .map(response -> new ApiResponse<Request>().create(response))
                        .onErrorReturn(throwable -> new ApiResponse<Request>().create(throwable))
        );

        results.addSource(apiResponse, requestObjectApiResponse -> {
            results.removeSource(dbSource);
            results.removeSource(apiResponse);

            if (requestObjectApiResponse instanceof ApiResponse.ApiSuccessResponse) {

                appExecutors.diskIO().execute(() -> {

                    saveCallResult((((ApiResponse.ApiSuccessResponse<Request>) requestObjectApiResponse).getBody()));

                    appExecutors.mainThread().execute(new Runnable() {
                        @Override
                        public void run() {
                            results.addSource(LiveDataReactiveStreams.fromPublisher(loadFromDb()), new Observer<Result>() {
                                @Override
                                public void onChanged(@Nullable Result result) {
                                    setValue(Resource.success(result));
                                }
                            });
                        }
                    });
                });
            } else if (requestObjectApiResponse instanceof ApiResponse.ApiEmptyResponse) {
                appExecutors.mainThread().execute(() -> results.addSource(LiveDataReactiveStreams.fromPublisher(loadFromDb()), new Observer<Result>() {
                    @Override
                    public void onChanged(@Nullable Result result) {
                        setValue(Resource.success(result));
                    }
                }));
            } else if (requestObjectApiResponse instanceof ApiResponse.ApiErrorResponse) {
                results.addSource(dbSource, result -> setValue(
                        Resource.error(
                                result,
                                ((ApiResponse.ApiErrorResponse<Request>) requestObjectApiResponse).getMsg()
                        )
                ));
            }
        });
    }

    private void setValue(Resource<Result> newValue) {
        if (results.getValue() != newValue) {
            results.setValue(newValue);
        }
    }

    @WorkerThread
    protected abstract void saveCallResult(@NonNull Request item);

    @MainThread
    protected abstract boolean shouldFetch(@Nullable Result data);

    @NonNull
    @MainThread
    protected abstract Flowable<Result> loadFromDb();

    @NonNull
    @MainThread
    protected abstract Flowable<Response<Request>> createCall();

    public final LiveData<Resource<Result>> getAsLiveData() {
        return results;
    }

}
