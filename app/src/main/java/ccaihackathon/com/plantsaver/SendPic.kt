package ccaihackathon.com.plantsaver

import okhttp3.MultipartBody
import java.io.File

data class SendPic (
    val file : MultipartBody.Part
)