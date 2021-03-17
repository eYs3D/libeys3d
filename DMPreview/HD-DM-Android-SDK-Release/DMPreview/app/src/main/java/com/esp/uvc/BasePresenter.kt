/*****************************************************************************/
/**
 * @date     Dec 2019
 *
 * @copyright (c) 2019 - Seven Sensing Software
 *
 */
/*****************************************************************************/

package com.esp.uvc

import org.koin.core.KoinComponent


/**
 * Interfaced Presenter apart of the base MVP abstraction.
 * It allows the attaching & un attaching of views.
 * The KoinComponent allows for DI within all presenters
 * @param T represent the view that it will bind to (Fragment, Activity ...)
 * @property view T
 */

interface BasePresenter<T> : KoinComponent {
    var view: T
    fun attach(view: T)
    fun unattach()
}