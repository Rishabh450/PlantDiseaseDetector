package ccaihackathon.com.plantsaver

interface RetrofitCall<T> {

    fun cancel()
    fun enqueue(callback: ResponseCallback<T>?)
    fun clone(): RetrofitCall<T>
}