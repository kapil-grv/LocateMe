package grv.locateme;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.POST;

public interface ApiService {

    @POST("/send_email")
    Call<String> sendEmail(@Body EmailRequest emailRequest);
}
