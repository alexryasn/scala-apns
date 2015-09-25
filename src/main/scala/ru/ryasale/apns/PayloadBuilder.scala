package ru.ryasale.apns

import ru.ryasale.apns.internal.Utilities

import scala.collection.mutable

import com.fasterxml.jackson.databind.ObjectMapper

/**
 * !Ready!
 * Created by ryasale on 24.09.15.
 *
 * Represents a builder for constructing Payload requests, as
 * specified by Apple Push Notification Programming Guide.
 */
class PayloadBuilder(var root: mutable.Map[String, Any] = new mutable.HashMap[String, Any],
                     var aps: mutable.Map[String, Any] = new mutable.HashMap[String, Any],
                     var customAlert: mutable.Map[String, Any] = new mutable.HashMap[String, Any]) {

  val mapper = new ObjectMapper()

  /**
   * Constructs a new instance of {@code PayloadBuilder}
   */
  root = new mutable.HashMap[String, Any] ++ root
  aps = new mutable.HashMap[String, Any] ++ aps
  customAlert = new mutable.HashMap[String, Any] ++ customAlert

  /**
   * Sets the alert body text, the text the appears to the user,
   * to the passed value
   *
   * @param alert the text to appear to the user
   * @return  this
   */
  def alertBody(alert: String) = {
    customAlert.put("body", alert)
    this
  }

  /**
   * Sets the alert title text, the text the appears to the user,
   * to the passed value.
   *
   * Used on iOS 8.2, iWatch and also Safari
   *
   * @param title the text to appear to the user
   * @return  this
   */
  def alertTitle(title: String) = {
    customAlert.put("title", title)
    this
  }

  /**
   * The key to a title string in the Localizable.strings file for the current localization.
   *
   * @param key  the localizable message title key
   * @return  this
   */
  def localizedTitleKey(key: String) = {
    customAlert.put("title-loc-key", key)
    this
  }

  /**
   * Sets the arguments for the localizable title key.
   *
   * @param arguments the arguments to the localized alert message
   * @return  this
   */
  def localizedTitleArguments(arguments: List[String]) = {
    customAlert.put("title-loc-args", arguments)
    this
  }

  /**
   * Sets the arguments for the localizable title key.
   *
   * @param arguments the arguments to the localized alert message
   * @return  this
   */
  def localizedTitleArguments(arguments: String*): PayloadBuilder = {
    localizedTitleArguments(arguments.toList)
  }

  /**
   * Sets the alert action text
   *
   * @param action The label of the action button
   * @return  this
   */
  def alertAction(action: String) = {
    customAlert.put("action", action)
    this
  }

  /**
   * Sets the "url-args" key that are paired with the placeholders
   * inside the urlFormatString value of your website.json file.
   * The order of the placeholders in the URL format string determines
   * the order of the values supplied by the url-args array.
   *
   * @param urlArgs the values to be paired with the placeholders inside
   *                the urlFormatString value of your website.json file.
   * @return  this
   */
  def urlArgs(urlArgs: String*) = {
    aps.put("url-args", urlArgs)
    this
  }

  /**
   * Sets the alert sound to be played.
   *
   * Passing {@code null} disables the notification sound.
   *
   * @param sound the file name or song name to be played
   *              when receiving the notification
   * @return  this
   */
  def sound(sound: String) = {
    if (sound != null) {
      aps.put("sound", sound)
    } else {
      aps.remove("sound")
    }
    this
  }

  /**
   * Sets the category of the notification for iOS8 notification
   * actions.  See 13 minutes into "What's new in iOS Notifications"
   *
   * Passing {@code null} removes the category.
   *
   * @param category the name of the category supplied to the app
   *                 when receiving the notification
   * @return  this
   */
  def category(category: String) = {
    if (category != null) {
      aps.put("category", category)
    } else {
      aps.remove("category")
    }
    this
  }

  /**
   * Sets the notification badge to be displayed next to the
   * application icon.
   *
   * The passed value is the value that should be displayed
   * (it will be added to the previous badge number), and
   * a badge of 0 clears the badge indicator.
   *
   * @param badge the badge number to be displayed
   * @return  this
   */
  def badge(badge: Int): PayloadBuilder = {
    aps += "badge" -> badge
    this
  }

  /**
   * Requests clearing of the badge number next to the application
   * icon.
   *
   * This is an alias to {@code badge(0)}.
   *
   * @return this
   */
  def clearBadge() = {
    badge(0)
  }

  /**
   * Sets the value of action button (the right button to be
   * displayed).  The default value is "View".
   *
   * The value can be either the simple String to be displayed or
   * a localizable key, and the iPhone will show the appropriate
   * localized message.
   *
   * A {@code null} actionKey indicates no additional button
   * is displayed, just the Cancel button.
   *
   * @param actionKey the title of the additional button
   * @return  this
   */
  def actionKey(actionKey: String) = {
    customAlert.put("action-loc-key", actionKey)
    this
  }

  /**
   * Set the notification view to display an action button.
   *
   * This is an alias to {@code actionKey(null)}
   *
   * @return this
   */
  def noActionButton() = {
    actionKey(null)
  }

  /**
   * Sets the notification type to be a 'newstand' notification.
   *
   * A Newstand Notification targets the Newstands app so that the app
   * updates the subscription info and content.
   *
   * @return this
   */
  def forNewsstand() = {
    aps.put("content-available", 1)
    this
  }

  /**
   * With iOS7 it is possible to have the application wake up before the user opens the app.
   *
   * The same key-word can also be used to send 'silent' notifications. With these 'silent' notification
   * a different app delegate is being invoked, allowing the app to perform background tasks.
   *
   * @return this
   */
  def instantDeliveryOrSilentNotification() = {
    aps.put("content-available", 1)
    this
  }

  /**
   * Set the notification localized key for the alert body
   * message.
   *
   * @param key   the localizable message body key
   * @return  this
   */
  def localizedKey(key: String) = {
    customAlert.put("loc-key", key)
    this
  }

  /**
   * Sets the arguments for the alert message localizable message.
   *
   * The iPhone doesn't localize the arguments.
   *
   * @param arguments the arguments to the localized alert message
   * @return  this
   */
  def localizedArguments(arguments: List[String]) = {
    customAlert.put("loc-args", arguments)
    this
  }

  /**
   * Sets the arguments for the alert message localizable message.
   *
   * The iPhone doesn't localize the arguments.
   *
   * @param arguments the arguments to the localized alert message
   * @return  this
   */
  def localizedArguments(arguments: String*): PayloadBuilder = {
    localizedArguments(arguments.toList)
  }

  /**
   * Sets the launch image file for the push notification
   *
   * @param launchImage   the filename of the image file in the
   *                      application bundle.
   * @return  this
   */
  def launchImage(launchImage: String) = {
    customAlert.put("launch-image", launchImage)
    this
  }

  /**
   * Sets any application-specific custom fields.  The values
   * are presented to the application and the iPhone doesn't
   * display them automatically.
   *
   * This can be used to pass specific values (urls, ids, etc) to
   * the application in addition to the notification message
   * itself.
   *
   * @param key   the custom field name
   * @param value the custom field value
   * @return  this
   */
  def customField(key: String, value: Object) = {
    root.put(key, value)
    this
  }

  def mdm(s: String) = {
    customField("mdm", s)
  }

  /**
   * Set any application-specific custom fields.  These values
   * are presented to the application and the iPhone doesn't
   * display them automatically.
   *
   * This method *adds* the custom fields in the map to the
   * payload, and subsequent calls add but doesn't reset the
   * custom fields.
   *
   * @param values   the custom map
   * @return  this
   */
  def customFields(values: mutable.Map[String, _]) = {
    root ++ values
    this
  }

  /**
   * Returns the length of payload bytes once marshaled to bytes
   *
   * @return the length of the payload
   */
  def length = {
    copy().buildBytes().length
  }

  /**
   * Returns true if the payload built so far is larger than
   * the size permitted by Apple (which is 2048 bytes).
   *
   * @return true if the result payload is too long
   */
  def isTooLong = {
    length > Utilities.MAX_PAYLOAD_LENGTH
  }

  /**
   * Shrinks the alert message body so that the resulting payload
   * message fits within the passed expected payload length.
   *
   * This method performs best-effort approach, and its behavior
   * is unspecified when handling alerts where the payload
   * without body is already longer than the permitted size, or
   * if the break occurs within word.
   *
   * @param payloadLength the expected max size of the payload
   * @return  this
   */
  def resizeAlertBody(payloadLength: Int): PayloadBuilder = {
    resizeAlertBody(payloadLength, "")
  }

  /**
   * Shrinks the alert message body so that the resulting payload
   * message fits within the passed expected payload length.
   *
   * This method performs best-effort approach, and its behavior
   * is unspecified when handling alerts where the payload
   * without body is already longer than the permitted size, or
   * if the break occurs within word.
   *
   * @param payloadLength the expected max size of the payload
   * @param postfix for the truncated body, e.g. "..."
   * @return  this
   */
  def resizeAlertBody(payloadLength: Int, postfix: String): PayloadBuilder = {
    var currLength = length
    if (currLength <= payloadLength) {
      return this
    }

    // now we are sure that truncation is required
    var body: String = customAlert.get("body").toString

    val acceptableSize = Utilities.toUTF8Bytes(body).length
    -(currLength - payloadLength
      + Utilities.toUTF8Bytes(postfix).length)
    body = Utilities.truncateWhenUTF8(body, acceptableSize) + postfix

    // set it back
    customAlert.put("body", body)

    // calculate the length again
    currLength = length

    if (currLength > payloadLength) {
      // string is still too long, just remove the body as the body is
      // anyway not the cause OR the postfix might be too long
      customAlert.remove("body")
    }

    this
  }

  /**
   * Shrinks the alert message body so that the resulting payload
   * message fits within require Apple specification (2048 bytes).
   *
   * This method performs best-effort approach, and its behavior
   * is unspecified when handling alerts where the payload
   * without body is already longer than the permitted size, or
   * if the break occurs within word.
   *
   * @return  this
   */
  def shrinkBody(): PayloadBuilder = {
    shrinkBody("")
  }

  /**
   * Shrinks the alert message body so that the resulting payload
   * message fits within require Apple specification (2048 bytes).
   *
   * This method performs best-effort approach, and its behavior
   * is unspecified when handling alerts where the payload
   * without body is already longer than the permitted size, or
   * if the break occurs within word.
   *
   * @param postfix for the truncated body, e.g. "..."
   *
   * @return  this
   */
  def shrinkBody(postfix: String) = {
    resizeAlertBody(Utilities.MAX_PAYLOAD_LENGTH, postfix)
  }

  /**
   * Returns the JSON String representation of the payload
   * according to Apple APNS specification
   *
   * @return  the String representation as expected by Apple
   */
  def build() = {
    if (!root.contains("mdm")) {
      insertCustomAlert()
      root.put("aps", aps)
    }
    try {
      mapper.writeValueAsString(root)
    } catch {
      case e: Exception => throw new RuntimeException(e)
    }
  }

  // TODO break isn't working!
  def insertCustomAlert() {
    customAlert.size match {
      case 0 => aps.remove("alert")
      //break;
      case 1 =>
        if (customAlert.contains("body")) {
          aps.put("alert", customAlert.get("body"))
          //break
        }
      // else follow through
      //$FALL-THROUGH$
      case _ =>
        aps.put("alert", customAlert)
    }
  }

  /**
   * Returns the bytes representation of the payload according to
   * Apple APNS specification
   *
   * @return the bytes as expected by Apple
   */
  def buildBytes() = {
    Utilities.toUTF8Bytes(build())
  }

  override def toString = {
    build()
  }

  /**
   * Returns a copy of this builder
   *
   * @return a copy of this builder
   */
  def copy() = {
    new PayloadBuilder(root, aps, customAlert)
  }


}

object PayloadBuilder {
  /**
   * @return a new instance of Payload Builder
   */
  def newPayload() = {
    new PayloadBuilder()
  }
}
