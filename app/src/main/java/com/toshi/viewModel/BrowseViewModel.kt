/*
 * 	Copyright (c) 2017. Toshi Inc
 *
 * 	This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.toshi.viewModel

import android.arch.lifecycle.MutableLiveData
import android.arch.lifecycle.ViewModel
import com.toshi.model.local.ToshiEntity
import com.toshi.model.local.User
import com.toshi.model.network.App
import com.toshi.util.LogUtil
import com.toshi.util.SingleLiveEvent
import com.toshi.view.BaseApplication
import rx.android.schedulers.AndroidSchedulers
import rx.subscriptions.CompositeSubscription

class BrowseViewModel : ViewModel() {

    private val subscriptions by lazy { CompositeSubscription() }

    val search by lazy { SingleLiveEvent<List<ToshiEntity>>() }
    val topRatedApps by lazy { MutableLiveData<List<App>>() }
    val featuredApps by lazy { MutableLiveData<List<App>>() }
    val topRatedPublicUsers by lazy { MutableLiveData<List<User>>() }
    val latestPublicUsers by lazy { MutableLiveData<List<User>>() }

    init {
        fetchTopRatedApps()
        fetchFeaturedApps()
        fetchTopRatedPublicUsers()
        fetchLatestPublicUsers()
    }

    private fun fetchTopRatedApps() {
        val sub = getAppManager()
                .getTopRatedApps(10)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                        { topRatedApps.value = it },
                        { LogUtil.exception(javaClass, "Error while fetching top rated apps $it") }
                )

        this.subscriptions.add(sub)
    }

    private fun fetchFeaturedApps() {
        val sub = getAppManager()
                .getLatestApps(10)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                        { featuredApps.value = it },
                        { LogUtil.exception(javaClass, "Error while fetching featured apps $it") }
                )

        this.subscriptions.add(sub)
    }

    private fun getAppManager() = BaseApplication.get().appsManager

    private fun fetchTopRatedPublicUsers() {
        val sub = getUserManager()
                .getTopRatedPublicUsers(10)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                        { topRatedPublicUsers.value = it },
                        { LogUtil.exception(javaClass, "Error while fetching top rated public users $it") }
                )

        this.subscriptions.add(sub)
    }

    private fun fetchLatestPublicUsers() {
        val sub = getUserManager()
                .getLatestPublicUsers(10)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                        { latestPublicUsers.value = it },
                        { LogUtil.exception(javaClass, "Error while fetching public users $it") }
                )

        this.subscriptions.add(sub)
    }

    private fun getUserManager() = BaseApplication.get().userManager

    fun runSearchQuery(query: String) {
        if (query.isEmpty()) return
        val sub = getRecipientManager()
                .searchOnlineUsersAndApps(query)
                .observeOn(AndroidSchedulers.mainThread())
                .map { users -> ArrayList<ToshiEntity>(users) }
                .subscribe(
                        { search.value = it },
                        { LogUtil.exception(javaClass, "Error while searching for app $it") }
                )

        this.subscriptions.add(sub)
    }

    private fun getRecipientManager() = BaseApplication.get().recipientManager

    override fun onCleared() {
        super.onCleared()
        subscriptions.clear()
    }
}