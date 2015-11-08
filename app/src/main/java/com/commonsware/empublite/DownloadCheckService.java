package com.commonsware.empublite;

import android.app.IntentService;
import android.content.Intent;
import android.util.Log;
import com.commonsware.cwac.security.ZipUtils;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.Response;
import de.greenrobot.event.EventBus;
import java.io.File;
import java.io.IOException;
import okio.Okio;
import okio.BufferedSink;
import retrofit.Call;
import retrofit.Retrofit;
import retrofit.GsonConverterFactory;

public class DownloadCheckService extends IntentService {
    public static final String UPDATE_BASEDIR = "updates";

    private static final String OUR_BOOK_DATE = "20120418";
    private static final String UPDATE_FILENAME = "book.zip";

    public DownloadCheckService() {
        super("DownloadCheckService");
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        try {
            String url = getUpdateUrl();

            if (url != null) {
                File book = download(url);
                File updateDir = new File(getFilesDir(), UPDATE_BASEDIR);

                updateDir.mkdirs();
                ZipUtils.unzip(book, updateDir);
                book.delete();
                EventBus.getDefault().post(new BookUpdatedEvent());
            }
        }
        catch (Exception e){
            Log.e(getClass().getSimpleName(), "Exception downloading updates", e);
        }
    }

    private String getUpdateUrl() throws IOException {
        Retrofit retrofit = new Retrofit.Builder()
            .baseUrl("https://commonsware.com")
            .addConverterFactory(GsonConverterFactory.create())
            .build();
        BookUpdateInterface updateInterface = retrofit.create(BookUpdateInterface.class);
        Call<BookUpdateInfo> call = updateInterface.update();
        BookUpdateInfo info = call.execute().body();

        if (info.updatedOn.compareTo(OUR_BOOK_DATE) > 0) {
            return info.updateUrl;
        }

        return null;
    }

    private File download(String url) throws IOException {
        File output = new File(getFilesDir(), UPDATE_FILENAME);

        if (output.exists()) {
            output.delete();
        }

        OkHttpClient client = new OkHttpClient();
        Request request = new Request.Builder().url(url).build();
        Response response = client.newCall(request).execute();
        BufferedSink sink = Okio.buffer(Okio.sink(output));

        sink.writeAll(response.body().source());
        sink.close();

        return output;
    }
}
