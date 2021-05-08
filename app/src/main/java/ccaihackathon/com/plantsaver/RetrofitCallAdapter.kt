package ccaihackathon.com.plantsaver

import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.io.IOException
import java.util.concurrent.Executor


internal class RetrofitCallAdapter<T>(
    private val call: Call<T>,
    private val callbackExecutor: Executor
) :
    RetrofitCall<T> {
    override fun cancel() {
        call.cancel()
    }

    override fun enqueue(callback: ResponseCallback<T>?) {
        call.enqueue(object : Callback<T> {
            override fun onResponse(
                call: Call<T>,
                response: Response<T>
            ) {
                // TODO if 'callbackExecutor' is not null, the 'callback' methods should be executed
                // on that executor by submitting a Runnable. This is left as an exercise for the reader.
                val code = response.code()
                if (code >= 200 && code < 300) {
                    callback!!.onSuccess(response)
                } else if (code == 401) {
                    callback!!.unauthenticated(response)
                } else if (code >= 400 && code < 500) {
                    callback!!.clientError(response)
                } else if (code >= 500 && code < 600) {
                    callback!!.serverError(response)
                } else {
                    callback!!.unexpectedError(RuntimeException("Unexpected response $response"))
                }
            }

            override fun onFailure(
                call: Call<T>,
                t: Throwable
            ) {
                // TODO if 'callbackExecutor' is not null, the 'callback' methods should be executed
                // on that executor by submitting a Runnable. This is left as an exercise for the reader.
                if (t is IOException) {
                    callback!!.networkError(t)
                } else {
                    callback!!.unexpectedError(t)
                }
            }
        })
    }

    override fun clone(): RetrofitCall<T> {
        return RetrofitCallAdapter(call.clone(), callbackExecutor)
    }

}