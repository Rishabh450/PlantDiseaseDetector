package ccaihackathon.com.plantsaver

import android.telecom.Call
import io.reactivex.Single
import okhttp3.MultipartBody
import okhttp3.ResponseBody
import retrofit2.http.Body
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part

interface LoginInterface {
    @Multipart
    @POST("predict")
    fun sendData(@Part part: MultipartBody.Part ): retrofit2.Call<ResponseBody>
}