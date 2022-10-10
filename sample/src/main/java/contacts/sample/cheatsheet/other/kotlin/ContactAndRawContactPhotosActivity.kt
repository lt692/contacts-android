package contacts.sample.cheatsheet.other.kotlin

import android.app.Activity
import android.graphics.Bitmap
import android.net.Uri
import contacts.core.Contacts
import contacts.core.entities.Contact
import contacts.core.entities.RawContact
import contacts.core.util.*

class ContactAndRawContactPhotosActivity : Activity() {

    fun getContactPhotoUri(contact: Contact): Uri? = contact.photoUri

    fun getContactThumbnailPhotoUri(contact: Contact): Uri? = contact.photoThumbnailUri

    fun getContactPhoto(contact: Contact): Bitmap? = contact.photoBitmap(Contacts(this))

    fun getContactPhotoThumbnail(contact: Contact): Bitmap? =
        contact.photoThumbnailBitmap(Contacts(this))

    fun getRawContactPhoto(rawContact: RawContact): Bitmap? = rawContact.photoBitmap(Contacts(this))

    fun getRawContactPhotoThumbnail(rawContact: RawContact): Bitmap? =
        rawContact.photoThumbnailBitmap(Contacts(this))

    fun setContactPhoto(contact: Contact, bitmap: Bitmap): Boolean =
        contact.setPhotoDirect(Contacts(this), PhotoData.from(bitmap))

    fun setRawContactPhoto(rawContact: RawContact, bitmap: Bitmap): Boolean =
        rawContact.setPhotoDirect(Contacts(this), PhotoData.from(bitmap))

    fun removeContactPhoto(contact: Contact): Boolean = contact.removePhotoDirect(Contacts(this))

    fun removeRawContactPhoto(rawContact: RawContact): Boolean =
        rawContact.removePhotoDirect(Contacts(this))
}