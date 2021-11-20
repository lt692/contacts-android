package contacts.core.profile

import android.accounts.Account
import android.content.ContentResolver
import android.provider.ContactsContract
import contacts.core.*
import contacts.core.entities.MutableRawContact
import contacts.core.entities.cursor.rawContactsCursor
import contacts.core.entities.table.ProfileUris
import contacts.core.util.isEmpty
import contacts.core.util.nullIfNotInSystem
import contacts.core.util.query
import contacts.core.util.toRawContactsWhere

/**
 * Inserts one (Profile) raw contact into the RawContacts table and all associated Data to the Data
 * table. The RawContact and Data table rows inserted here are stored in a special part of the
 * respective tables and are not visible via regular queries. Use [ProfileQuery] for retrieval.
 *
 * If the (Profile) Contact does not yet exist, one will be created. Otherwise, the raw contact will
 * be automatically associated with / belong to the (Profile) Contact upon creation. Note that there
 * is zero or one (Profile) Contact, which may have one or more RawContacts.
 *
 * The native Contacts app typically only maintains one local (no account) RawContact when
 * configuring the user's profile.
 *
 * ## Permissions
 *
 * The [ContactsPermissions.WRITE_PERMISSION] and
 * [contacts.core.accounts.AccountsPermissions.GET_ACCOUNTS_PERMISSION] are assumed to have been
 * granted already in these examples for brevity. All inserts will do nothing if these permissions
 * are not granted.
 *
 * For API 22 and below, the permission "android.permission.WRITE_PROFILE" is also required.
 *
 * ## Usage
 *
 * To insert a (Profile) raw contact with the name "john doe" with email "john@doe.com" for the
 * local account (no account), not allowing multiple raw contacts per account;
 *
 * In Kotlin,
 *
 * ```kotlin
 * val result = profileInsert
 *      .rawContact {
 *          name = MutableName().apply {
 *              givenName = "john"
 *              familyName = "doe"
 *          }
 *          emails.add(MutableEmail().apply {
 *              type = Email.Type.HOME
 *              address = "john@doe.com"
 *          })
 *      }
 *      .commit()
 * ```
 *
 * In Java,
 *
 * ```java
 * MutableName name = new MutableName();
 * name.setGivenName("john");
 * name.setFamilyName("doe");
 *
 * MutableEmail email = new MutableEmail();
 * email.setType(Email.Type.HOME);
 * email.setAddress("john@doe.com");
 *
 * List<MutableEmail> emails = new ArrayList<>();
 * emails.add(email);
 *
 * MutableRawContact rawContact = new MutableRawContact();
 * rawContact.setName(name);
 * rawContact.setEmails(emails);
 *
 * ProfileInsert.Result result = profileInsert
 *      .rawContact(rawContact)
 *      .commit();
 * ```
 */
interface ProfileInsert {

    /**
     * If [allowBlanks] is set to true, then blank RawContacts ([MutableRawContact.isBlank]) will
     * will be inserted. Otherwise, blanks will not be inserted and will result in a failed
     * operation. This flag is set to false by default.
     *
     * The Contacts Providers allows for RawContacts that have no rows in the Data table (let's call
     * them "blanks") to exist. The native Contacts app does not allow insertion of new RawContacts
     * without at least one data row. It also deletes blanks on update. Despite seemingly not
     * allowing blanks, the native Contacts app shows them.
     */
    fun allowBlanks(allowBlanks: Boolean): ProfileInsert

    /**
     * If [allowMultipleRawContactsPerAccount] is set to true, then inserting a profile RawContact
     * with an Account that already has a profile RawContact is allowed. Otherwise, this will result
     * in a failed operation. This flag is set to false by default.
     *
     * According to the `ContactsContract.Profile` documentation; "... each account (including data
     * set, if applicable) on the device may contribute a single raw contact representing the user's
     * personal profile data from that source." In other words, one account can have one profile
     * RawContact.
     *
     * Despite the documentation of "one profile RawContact per one Account", the Contacts Provider
     * allows for multiple RawContacts per Account, including multiple local RawContacts (no
     * Account).
     */
    fun allowMultipleRawContactsPerAccount(
        allowMultipleRawContactsPerAccount: Boolean
    ): ProfileInsert

    /**
     * The RawContact that is inserted on [commit] will belong to the given [account].
     *
     * If not provided, or null is provided, or if an incorrect account is provided, the raw
     * contacts inserted here will not be associated with an account. RawContacts inserted without
     * an associated account are considered local or device-only contacts, which are not synced.
     *
     * **For Lollipop (API 22) and below**
     *
     * When an Account is added, from a state where no accounts have yet been added to the system, the
     * Contacts Provider automatically sets all of the null `accountName` and `accountType` in the
     * RawContacts table to that Account's name and type.
     *
     * RawContacts inserted without an associated account will automatically get assigned to an account
     * if there are any available. This may take a few seconds, whenever the Contacts Provider decides
     * to do it.
     *
     * **For Marshmallow (API 23) and above**
     *
     * The Contacts Provider no longer associates local contacts to an account when an account is or
     * becomes available. Local contacts remain local.
     *
     * **Account removal**
     *
     * Removing the Account will delete all of the associated rows in the Contact, RawContact, and
     * Data tables.
     */
    fun forAccount(account: Account?): ProfileInsert

    /**
     * Specifies that only the given set of [fields] (data) will be inserted.
     *
     * If no fields are specified, then all fields will be inserted. Otherwise, only the specified
     * fields will be inserted.
     *
     * ## Note
     *
     * The use case for this function is probably not common. You can simply not set a particular
     * data instead of using this function. For example, if you want to create a new RawContact
     * with only name and email data, just set only name and email...
     *
     * There may be some cases where this function may come in handy. For example, if you have a
     * mutable RawContact that has all data filled in but you only want some of those data to be
     * inserted (in the database), then this function is exactly what you need =) This can also come
     * in handy if you are trying to make copies of an existing RawContact but only want some data
     * to be copied.
     */
    fun include(vararg fields: AbstractDataField): ProfileInsert

    /**
     * See [ProfileInsert.include].
     */
    fun include(fields: Collection<AbstractDataField>): ProfileInsert

    /**
     * See [ProfileInsert.include].
     */
    fun include(fields: Sequence<AbstractDataField>): ProfileInsert

    /**
     * Configures a new [MutableRawContact] for insertion, which will be inserted on [commit]. The
     * new instance is configured by the [configureRawContact] function.
     *
     * Replaces any previously set RawContact in the insert queue.
     */
    fun rawContact(configureRawContact: MutableRawContact.() -> Unit): ProfileInsert

    /**
     * Sets the given [rawContact] for insertion, which will be inserted on [commit].
     *
     * Replaces any previously set RawContact in the insert queue.
     */
    fun rawContact(rawContact: MutableRawContact): ProfileInsert

    /**
     * Inserts the [MutableRawContact]s in the queue (added via [rawContact]) and returns the
     * [Result].
     *
     * ## Permissions
     *
     * Requires [ContactsPermissions.WRITE_PERMISSION] and
     * [contacts.core.accounts.AccountsPermissions.GET_ACCOUNTS_PERMISSION].
     *
     * ## Thread Safety
     *
     * This should be called in a background thread to avoid blocking the UI thread.
     */
    // [ANDROID X] @WorkerThread (not using annotation to avoid dependency on androidx.annotation)
    fun commit(): Result

    /**
     * Inserts the [MutableRawContact]s in the queue (added via [rawContact]) and returns the
     * [Result].
     *
     * ## Permissions
     *
     * Requires [ContactsPermissions.WRITE_PERMISSION] and
     * [contacts.core.accounts.AccountsPermissions.GET_ACCOUNTS_PERMISSION].
     *
     * ## Cancellation
     *
     * To cancel at any time, the [cancel] function should return true.
     *
     * This is useful when running this function in a background thread or coroutine.
     *
     * **Cancelling does not undo insertions. This means that depending on when the cancellation
     * the RawContact in the insert queue may have already been inserted.**
     *
     * ## Thread Safety
     *
     * This should be called in a background thread to avoid blocking the UI thread.
     */
    // [ANDROID X] @WorkerThread (not using annotation to avoid dependency on androidx.annotation)
    // @JvmOverloads cannot be used in interface methods...
    // fun commit(cancel: () -> Boolean = { false }): Result
    fun commit(cancel: () -> Boolean): Result

    interface Result {

        /**
         * The ID of the successfully created RawContact. Null if the insertion failed.
         */
        val rawContactId: Long?

        /**
         * True if the MutableRawContact has successfully been inserted. False if insertion failed.
         */
        val isSuccessful: Boolean
    }
}

@Suppress("FunctionName")
internal fun ProfileInsert(contacts: Contacts): ProfileInsert = ProfileInsertImpl(contacts)

private class ProfileInsertImpl(
    private val contacts: Contacts,

    private var allowBlanks: Boolean = false,
    private var allowMultipleRawContactsPerAccount: Boolean = false,
    private var include: Include<AbstractDataField> = allDataFields(contacts.customDataRegistry),
    private var account: Account? = null,
    private var rawContact: MutableRawContact? = null
) : ProfileInsert {

    override fun toString(): String =
        """
            ProfileInsert {
                allowBlanks: $allowBlanks
                allowMultipleRawContactsPerAccount: $allowMultipleRawContactsPerAccount
                include: $include
                account: $account
                rawContact: $rawContact
            }
        """.trimIndent()

    override fun allowBlanks(allowBlanks: Boolean): ProfileInsert = apply {
        this.allowBlanks = allowBlanks
    }

    override fun allowMultipleRawContactsPerAccount(
        allowMultipleRawContactsPerAccount: Boolean
    ): ProfileInsert = apply {
        this.allowMultipleRawContactsPerAccount = allowMultipleRawContactsPerAccount
    }

    override fun forAccount(account: Account?): ProfileInsert = apply {
        this.account = account
    }

    override fun include(vararg fields: AbstractDataField) = include(fields.asSequence())

    override fun include(fields: Collection<AbstractDataField>) = include(fields.asSequence())

    override fun include(fields: Sequence<AbstractDataField>): ProfileInsert = apply {
        include = if (fields.isEmpty()) {
            allDataFields(contacts.customDataRegistry)
        } else {
            Include(fields + Fields.Required.all.asSequence())
        }
    }

    override fun rawContact(configureRawContact: MutableRawContact.() -> Unit): ProfileInsert =
        rawContact(MutableRawContact().apply(configureRawContact))

    override fun rawContact(rawContact: MutableRawContact): ProfileInsert = apply {
        this.rawContact = rawContact
    }

    override fun commit(): ProfileInsert.Result = commit { false }

    override fun commit(cancel: () -> Boolean): ProfileInsert.Result {
        val rawContact = rawContact

        if (rawContact == null
            || (!allowBlanks && rawContact.isBlank)
            || !contacts.permissions.canInsert
            || cancel()
        ) {
            return ProfileInsertFailed()
        }

        // This ensures that a valid account is used. Otherwise, null is used.
        account = account?.nullIfNotInSystem(contacts.accounts())

        if (
            (!allowMultipleRawContactsPerAccount &&
                    contacts.applicationContext.contentResolver.hasProfileRawContactForAccount(
                        account
                    ))
            || cancel()
        ) {
            return ProfileInsertFailed()
        }

        // No need to propagate the cancel function to within insertRawContactForAccount
        // as that operation should be fast and CPU time should be trivial.
        val rawContactId =
            contacts.insertRawContactForAccount(account, include.fields, rawContact, IS_PROFILE)

        return ProfileInsertResult(rawContactId)
    }

    private companion object {
        const val IS_PROFILE = true
    }
}

private class ProfileInsertResult(override val rawContactId: Long?) : ProfileInsert.Result {

    override val isSuccessful: Boolean = rawContactId?.let(ContactsContract::isProfileId) == true
}

private class ProfileInsertFailed : ProfileInsert.Result {

    override val rawContactId: Long? = null

    override val isSuccessful: Boolean = false
}

private fun ContentResolver.hasProfileRawContactForAccount(account: Account?): Boolean = query(
    ProfileUris.RAW_CONTACTS.uri,
    Include(RawContactsFields.Id),
    // There may be lingering RawContacts whose associated contact was already deleted.
    // Such RawContacts have contact id column value as null.
    RawContactsFields.ContactId.isNotNull() and account.toRawContactsWhere()
) {
    it.getNextOrNull { it.rawContactsCursor().rawContactId } != null
} ?: false