@file:Suppress("unused", "MemberVisibilityCanBePrivate")

package studio.forface.viewstatestore

import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer

/**
 * This class will store and handle the [ViewState] and submit it via a [LiveData].
 * `ViewStateStore` will only deliver the last [data] when the observer become active.
 * This `ViewStateStore` is *locked* so `set` functions and `post` functions can only be called within [ViewStateStoreScope]
 *
 *
 * ### Publish:
 *
 * Use the [ViewStateStoreScope.setState] for deliver the [ViewState] on the current thread.
 * Use the [ViewStateStoreScope.postState] for deliver the [ViewState] on the main thread.
 * Use [state] for retrieve the last state.
 *
 *
 * ### Observe:
 *
 * Use [observe] for observe the published [ViewState] within a [LifecycleOwner]
 * Use [observeForever] for observe the published [ViewState]. NOTE! This will probably lead to a memory leak.
 *
 * Use [observeData] for observe ONLY the the published [data] within a [LifecycleOwner]
 * Use [observeForever] for observe ONLY the the published [data]. NOTE! This will probably lead to a memory leak.
 *
 * @see ViewStateObserver
 *
 *
 * This class is abstract and will be inherited from [AbsViewStateStore], that implements [ViewStateStoreScope] for
 * being able to call `set` functions and `post` functions without defining a [ViewStateStoreScope].
 *
 * @param V is the type of the [data]
 *
 * @param dropOnSame This [Boolean] defines whether a publishing should be dropped if the same [ViewState] is already
 * the last [state]
 * @see ViewStateStoreConfig.dropOnSame
 * Default value is inherited from [ViewStateStoreConfig.dropOnSame]
 *
 *
 * @author Davide Giuseppe Farella
 */
abstract class LockedViewStateStore<V>( internal val dropOnSame: Boolean ) {

    /**
     * This property will store the last available [ViewState.data], so it will be emitted every
     * time. It is useful if, for instance, we have a failure after rotating the screen: in
     * that case the data would not be emitter.
     */
    private var data: V? = null

    /**
     * This property will store the last [ViewState.Error], so it won't be delivered every
     * time for every new [Observer] so, if for instance we rotate the screen, the last
     * error won't be delivered again.
     */
    private var lastError: ViewState.Error? = null

    /** An instance of [MutableLiveData] of [ViewState] of [V] for dispatch [ViewState]'s */
    @PublishedApi
    internal open val liveData = MutableLiveData<ViewState<V>>()

    /**
     * @return an instance of [Observer]
     * @see handleViewState
     */
    @PublishedApi
    internal fun observerWith( observer: ViewStateObserver<V> ) =
            Observer<ViewState<V>> { viewState -> handleViewState( observer, viewState ) }

    /**
     * Handle the delivery of a [ViewState] though [ViewStateObserver]
     *
     * This will save [ViewState.data] in case of success and then trigger the callbacks the following callbacks on the
     * given [ViewStateObserver]: [ViewStateObserver.onEach], [ViewStateObserver.onData], [ViewStateObserver.onError]
     * and [ViewStateObserver.onLoadingChange].
     */
    protected fun handleViewState(
        observer: ViewStateObserver<V>,
        viewState: ViewState<V>
    ) = with( observer ) {
        // Deliver every ViewState.
        onEach( viewState )

        // Every time the observer is triggered for any reason ( loading change, data or error ),
        // if ViewState is Success, we store the new data then, in every case, if data is not
        // null, we deliver the data.
        viewState.doOnData { data = it }
        data?.let( onData )

        // Every time the observer is triggered for any reason ( loading change, data or error ),
        // we instantiate a new NULL ViewState.Error on newError then, if ViewState is Error
        // we store the value in newError then, if error is different from lastError, we
        // deliver it if not Null and store in lastError.
        var newError: ViewState.Error? = null
        viewState.doOnError { error -> newError = error }
        if ( newError !== lastError ) {
            newError?.let( onError )
            lastError = newError
        }

        // If View.Stare is a loading change, we deliver it.
        viewState.doOnLoadingChange( onLoadingChange )
    }

    /**
     * @see LiveData.observe with an [Observer] created with an instance of [ViewStateObserver]
     * @param block a lambda with [ViewStateObserver] as receiver that properly sets callbacks on it.
     */
    inline fun observe( owner: LifecycleOwner, block: ViewStateObserver<V>.() -> Unit ) {
        val observer = `access$onCreateViewStateObserver`( owner )
        observer.block()
        liveData.observe( owner, observerWith( observer ) )
    }

    /**
     * Observe the [ViewStateStore] and trigger [block] only on [ViewStateObserver.onData]
     * @see LiveData.observe with an [Observer] created with an instance of [ViewStateObserver]
     * @param block a lambda with [ViewStateObserver] as receiver that properly sets callbacks on it.
     */
    inline fun observeData( owner: LifecycleOwner, crossinline block: (V) -> Unit ) {
        val observer = `access$onCreateViewStateObserver`( owner )
        observer.onData = { block( it ) }
        liveData.observe( owner, observerWith( observer ) )
    }

    /**
     * @see LiveData.observeForever with an [Observer] created with an instance of [ViewStateObserver]
     * @param block a lambda with [ViewStateObserver] as receiver that properly sets callbacks on it.
     */
    inline fun observeForever( block: ViewStateObserver<V>.() -> Unit ) {
        val observer = `access$onCreateViewStateObserver`()
        observer.block()
        liveData.observeForever( observerWith( observer ) )
    }

    /**
     * Observe the [ViewStateStore] and trigger [block] only on [ViewStateObserver.onData]
     * @see LiveData.observeForever with an [Observer] created with an instance of [ViewStateObserver]
     * @param block a lambda with [ViewStateObserver] as receiver that properly sets callbacks on it.
     */
    inline fun observeDataForever( crossinline block: (V) -> Unit ) {
        val observer = `access$onCreateViewStateObserver`()
        observer.onData = { block( it ) }
        liveData.observeForever( observerWith( observer ) )
    }

    /** @return a new instance of [ViewStateObserver] */
    protected open fun onCreateViewStateObserver( owner: LifecycleOwner? = null ) =
            ViewStateObserver<V>()

    /** @return [LiveData.getValue] on [liveData] */
    fun state() = liveData.value

    /**
     * @return [LiveData.getValue] asserted as non null
     * @throws KotlinNullPointerException if [state] is null.
     */
    fun unsafeState() = state()!!

    @PublishedApi
    @Suppress("FunctionName")
    internal fun `access$onCreateViewStateObserver`( owner: LifecycleOwner? = null ) =
        onCreateViewStateObserver( owner )
}