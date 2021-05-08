package ccaihackathon.com.plantsaver

import retrofit2.Call
import retrofit2.CallAdapter
import retrofit2.Retrofit
import java.lang.reflect.ParameterizedType
import java.lang.reflect.Type
import java.util.concurrent.Executor


class ErrorHandlingCallAdapterFactory : CallAdapter.Factory() {
    override fun get(
        returnType: Type,
        annotations: Array<Annotation>,
        retrofit: Retrofit
    ): CallAdapter<*, *>? {
        if (getRawType(returnType) != RetrofitCall::class.java) {
            return null
        }
        check(returnType is ParameterizedType) { "MyCall must have generic type (e.g., MyCall<ResponseBody>)" }
        val responseType =
            getParameterUpperBound(
                0,
                returnType
            )
        val callbackExecutor = retrofit.callbackExecutor()
        return ErrorHandlingCallAdapter<Any>(
            responseType,
            callbackExecutor
        )
    }

    private class ErrorHandlingCallAdapter<R> internal constructor(
        private val responseType: Type,
        private val callbackExecutor: Executor?
    ) :
        CallAdapter<R, RetrofitCall<R>> {
        override fun responseType(): Type {
            return responseType
        }

        override fun adapt(call: Call<R>): RetrofitCall<R> {
            return RetrofitCallAdapter(call, callbackExecutor!!)
        }

    }
}

