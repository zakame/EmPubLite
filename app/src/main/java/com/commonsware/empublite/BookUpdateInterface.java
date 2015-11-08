package com.commonsware.empublite;

import retrofit.Call;
import retrofit.http.GET;

public interface BookUpdateInterface {
    @GET("/misc/empublite-update.json")
    Call<BookUpdateInfo> update();
}
