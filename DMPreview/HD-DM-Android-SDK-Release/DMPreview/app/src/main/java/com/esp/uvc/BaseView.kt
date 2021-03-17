/*****************************************************************************/
/**
 * @date     Dec 2019
 *
 * @copyright (c) 2019 - Seven Sensing Software
 *
 */
/*****************************************************************************/

package com.esp.uvc


/**
 *  The BaseView apart of the MVP abstraction
 *  Usually implemented upon Fragments & Activity
 * @param T
 * @property presenter T
 */
interface BaseView<T> {
    val presenter: T
}