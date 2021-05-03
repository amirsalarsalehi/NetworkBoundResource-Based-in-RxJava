import androidx.lifecycle.LiveData;
import androidx.lifecycle.LiveDataReactiveStreams;
import androidx.lifecycle.MediatorLiveData;

import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.schedulers.Schedulers;
import retrofit2.Response;

public abstract class NetworkBoundResource<Result, Request> {

    private MediatorLiveData<Resource<Result>> results = new MediatorLiveData<>();

    public NetworkBoundResource() {
        init();
    }

    private void init() {
        results.setValue(Resource.loading(null));

        final LiveData<Result> dbSource = LiveDataReactiveStreams.fromPublisher(loadFromDb());

        results.addSource(dbSource, result -> {
            results.removeSource(dbSource);
            if (shouldFetch(result)) fetchFromNetwork(dbSource);
            else results.addSource(dbSource, res -> {
                results.removeSource(dbSource);
                setValue(Resource.success(res));
            });
        });
    }

    private void fetchFromNetwork(LiveData<Result> dbSource) {
        results.addSource(dbSource, result -> {
            results.removeSource(dbSource);
            setValue(Resource.loading(result));
        });

        final LiveData<ApiResponse<Request>> apiResponse = LiveDataReactiveStreams.fromPublisher(
                createCall()
                        .subscribeOn(Schedulers.io())
                        .observeOn(Schedulers.io())
                        .map(request -> new ApiResponse<Request>().create(request))
                        .onErrorReturn(throwable -> new ApiResponse<Request>().create(throwable))
        );


        results.addSource(apiResponse, requestApiResponse -> {

            results.removeSource(apiResponse);
            results.removeSource(dbSource);
            LiveData<Result> data;

            if (requestApiResponse instanceof ApiResponse.ApiSuccessResponse) {
                saveCallResult(((ApiResponse.ApiSuccessResponse<Request>) requestApiResponse).getBody());
                data = LiveDataReactiveStreams.fromPublisher(loadFromDb().onErrorReturn(throwable -> null));
                results.addSource(data, result -> {
                            setValue(Resource.success(result));
                            results.removeSource(data);
                        }
                );
            } else if (requestApiResponse instanceof ApiResponse.ApiEmptyResponse) {
                data = LiveDataReactiveStreams.fromPublisher(loadFromDb().onErrorReturn(throwable -> null));
                results.addSource(data, result -> {
                    setValue(Resource.success(result));
                    results.removeSource(data);
                });
            } else if (requestApiResponse instanceof ApiResponse.ApiErrorResponse) {
                results.addSource(dbSource, result -> {
                    setValue(
                            Resource.error(
                                    result,
                                    ((ApiResponse.ApiErrorResponse<Request>) requestApiResponse).getMsg()
                            )
                    );
                    results.removeSource(dbSource);
                });
            }
        });
    }

    private void setValue(Resource<Result> newValue) {
        if (results.getValue() != newValue)
            results.setValue(newValue);
    }

    public abstract void saveCallResult(Request item);

    public abstract boolean shouldFetch(Result item);

    public abstract Flowable<Result> loadFromDb();

    public abstract Flowable<Response<Request>> createCall();

    public MediatorLiveData<Resource<Result>> asLiveData() {
        return results;
    }
}
