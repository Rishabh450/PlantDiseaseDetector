package ccaihackathon.com.plantsaver

import okio.IOException
import retrofit2.Response

interface ResponseCallback<T> {
    fun onSuccess(response: Response<*>?)

    /** Called for 401 responses.  */
    fun unauthenticated(response: Response<*>?)

    /** Called for [400, 500) responses, except 401.  */
    fun clientError(response: Response<*>?)

    /** Called for [500, 600) response.  */
    fun serverError(response: Response<*>?)

    /** Called for network errors while making the call.  */
    fun networkError(e: IOException?)

    /** Called for unexpected errors while making the call.  */
    fun unexpectedError(t: Throwable?)
}