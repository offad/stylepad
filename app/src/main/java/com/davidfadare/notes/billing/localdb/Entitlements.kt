/**
 * For this app the decision was made to create an [Entity] class for each product and feature that
 * the app sells. It was further decided to put all of them in one file. You don't have to do it
 * this way. But here is why it's done that way here:
 *
 * 1 - The only way for the users to have offline access to their entitlements is if they are saved
 *     somehow. It is more convenient to use Room instead of, say, SharedPreferences.
 *
 * 2 - It may seem like an overkill to create an entire class just to persist a Boolean, as in the
 *      case of [GoldStatus]. Yes and no. Putting it inside Room means it can be tracked using
 *      LiveData without the clients having to write too much code. Also maybe later there will
 *      be needs to add more fields, such as the status of the subscription.
 *
 * 3 - Grouping all the app's products and subscriptions in one file called Entitlements seem
 *      effective.
 */
package com.davidfadare.notes.billing.localdb

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Normally this would just be an interface. But since each of the entitlements only has
 * one item/row and so primary key is fixed, we can put the primary key here and so make
 * the class abstract.
 **/
abstract class Entitlement {
    @PrimaryKey
    var id: Int = 1

    /**
     * This method tells clients whether a user __should__ buy a particular item at the moment. For
     * example, if the gas tank is full the user should not be buying gas. This method is __not__
     * a reflection on whether Google Play Billing can make a purchase.
     */
    abstract fun mayPurchase(): Boolean
}

/**
 * Indicates whether the user owns premium.
 */
@Entity(tableName = "premium")
data class Premium(val entitled: Boolean) : Entitlement() {
    override fun mayPurchase(): Boolean = !entitled
}

