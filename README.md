# NetworkBoundResource-Based-in-RxJava
NetworkBoundResource.java Based in RxJava for Local Db Caching for whos dont want to make Api and Database call as LiveData

## Easy To Use

- Add NetworkBoundResource.java to your project
- Make your Api and Database call return RxJava Observable, Flowable, Single ...
- Create method in your repository that return LiveData and use asLiveData to get result as LiveData
- Enjoy Local Database Cache !
 
## Example

``` Java
public LiveData<Resource<List<Recipe>>> getRecipes(String q, int num) {
        return new NetworkBoundResource<List<Recipe>, RecipeSearchResponse>() {
        
            @Override
            public void saveCallResult(RecipeSearchResponse item) {
                if (item != null) {
                    Recipe[] recipes = new Recipe[item.getRecipes().size()];
                    dao.insertRecipes(item.getRecipes().toArray(recipes));
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
```

