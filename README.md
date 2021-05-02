# NetworkBoundResource-Based-in-RxJava
NetworkBoundResource.java Based in RxJava for Local Db Caching

## Easy To Use
- Add NetworkBoundResource.java to your project
- Create method in your repository that return LiveData and use asLiveData to get result as LiveData
- Enjoy Local Database Cache !

----
public LiveData<Resource<List<Recipe>>> getRecipes(String q, int num) {
        return new NetworkBoundResource<List<Recipe>, RecipeSearchResponse>() {

            @Override
            public void saveCallResult(RecipeSearchResponse item) {
                if (item != null) {
                    Recipe[] recipes = new Recipe[item.getRecipes().size()];
                    dao.insertRecipes(item.getRecipes().toArray(recipes))
                            .subscribeOn(Schedulers.io())
                            .observeOn(Schedulers.io())
                            .subscribe(longs -> {
                                int index = 0;
                                for (long rowId : longs) {
                                    if (rowId == -1) {
                                        dao.updateRecipe(
                                                recipes[index].getRecipe_id(),
                                                recipes[index].getTitle(),
                                                recipes[index].getPublisher(),
                                                recipes[index].getImage_url(),
                                                recipes[index].getSocial_rank()
                                        );
                                    }
                                    index++;
                                }
                            });
                }
            }

            @Override
            public boolean shouldFetch(List<Recipe> item) {
                return true;
            }

            @Override
            public Flowable<List<Recipe>> loadFromDb() {
                return dao.searchRecipe(q, num);
            }

            @Override
            public Flowable<Response<RecipeSearchResponse>> createCall() {
                return ServiceGenerator.getRecipeApi().searchRecipe(q, num);
            }
        }.asLiveData();
----

