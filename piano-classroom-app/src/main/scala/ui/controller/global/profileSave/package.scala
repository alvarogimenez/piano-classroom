package ui.controller.global

package object profileSave {
  final val PROFILE_SAVE_MODAL_ACCEPT = 1
  final val PROFILE_SAVE_MODAL_CANCEL = 0

  object ProfileSaveAction extends Enumeration {
    type ProfileSaveAction = Value
    val OVERRIDE, NEW = Value
  }
}
