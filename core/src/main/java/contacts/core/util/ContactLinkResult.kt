package contacts.core.util

import contacts.core.Contacts
import contacts.core.aggregationexceptions.ContactLink
import contacts.core.entities.Contact
import contacts.core.equalTo

// Note that there is no need to handle isProfile here as ContactLinks operations do not support it.

/**
 * Returns the [Contact] that contains all of the successfully linked RawContacts. Returns null if
 * the link operation failed or permissions are not granted.
 *
 * ## Permissions
 *
 * The [contacts.core.ContactsPermissions.READ_PERMISSION] is required.
 *
 * ## Cancellation
 *
 * To cancel this operation at any time, the [cancel] function should return true.
 *
 * This is useful when running this function in a background thread or coroutine.
 *
 * ## Thread Safety
 *
 * This should be called in a background thread to avoid blocking the UI thread.
 */
// [ANDROID X] @WorkerThread (not using annotation to avoid dependency on androidx.annotation)
@JvmOverloads
fun ContactLink.Result.contact(contacts: Contacts, cancel: () -> Boolean = { false }): Contact? =
    contactId?.let {
        contacts.query()
            .where { Contact.Id equalTo it }
            .find(cancel)
            .firstOrNull()
    }