# Extract String Resources - Project Wide

This plan aims to extract all hardcoded strings from the entire project and move them into `strings.xml` for better maintainability and localization support.

## Proposed Changes

### [Resources]

#### [MODIFY] [strings.xml](file:///home/kumpel/AndroidStudioProjects/WhereIsEveryone2/app/src/main/res/values/strings.xml)
- Add new string resources for titles, messages, and content descriptions found in all UI components.

### [Authentication]

#### [MODIFY] [SignUpScreen.kt](file:///home/kumpel/AndroidStudioProjects/WhereIsEveryone2/app/src/main/java/com/kumpello/whereiseveryone/authentication/signUp/ui/SignUpScreen.kt)
- "Sign up", "Username", "Password", "Log in here", "Condition"

#### [MODIFY] [ConditionRow.kt](file:///home/kumpel/AndroidStudioProjects/WhereIsEveryone2/app/src/main/java/com/kumpello/whereiseveryone/authentication/signUp/ui/ConditionRow.kt)
- "text color", "status icon"

#### [MODIFY] [LoginScreen.kt](file:///home/kumpel/AndroidStudioProjects/WhereIsEveryone2/app/src/main/java/com/kumpello/whereiseveryone/authentication/login/ui/LoginScreen.kt)
- "Login", "Username", "Password", "Sign up here"

#### [MODIFY] [SplashScreen.kt](file:///home/kumpel/AndroidStudioProjects/WhereIsEveryone2/app/src/main/java/com/kumpello/whereiseveryone/authentication/splash/ui/SplashScreen.kt)
- "Splash screen"

#### [MODIFY] [TextField.kt](file:///home/kumpel/AndroidStudioProjects/WhereIsEveryone2/app/src/main/java/com/kumpello/whereiseveryone/authentication/common/ui/TextField.kt)
- "Hide password", "Show password"

### [Main Map]

#### [MODIFY] [Map.kt](file:///home/kumpel/AndroidStudioProjects/WhereIsEveryone2/app/src/main/java/com/kumpello/whereiseveryone/main/map/ui/Map.kt)
- "Map Placeholder"

#### [MODIFY] [MapBottomControls.kt](file:///home/kumpel/AndroidStudioProjects/WhereIsEveryone2/app/src/main/java/com/kumpello/whereiseveryone/main/map/ui/MapBottomControls.kt)
- "Zoom out", "Zoom in", "Center"

#### [MODIFY] [MapContent.kt](file:///home/kumpel/AndroidStudioProjects/WhereIsEveryone2/app/src/main/java/com/kumpello/whereiseveryone/main/map/ui/MapContent.kt)
- "Map Placeholder", "Direction to %s"

#### [MODIFY] [MapTopControls.kt](file:///home/kumpel/AndroidStudioProjects/WhereIsEveryone2/app/src/main/java/com/kumpello/whereiseveryone/main/map/ui/MapTopControls.kt)
- "Message", "Friends", "Settings"

#### [MODIFY] [MessageFloatingCard.kt](file:///home/kumpel/AndroidStudioProjects/WhereIsEveryone2/app/src/main/java/com/kumpello/whereiseveryone/main/map/ui/MessageFloatingCard.kt)
- "Status", "Draft"

### [Friends UI] (Carry over from previous plan)

#### [MODIFY] [AddFriendContent.kt](file:///home/kumpel/AndroidStudioProjects/WhereIsEveryone2/app/src/main/java/com/kumpello/whereiseveryone/main/friends/ui/AddFriendContent.kt)
#### [MODIFY] [DeleteFriendDialog.kt](file:///home/kumpel/AndroidStudioProjects/WhereIsEveryone2/app/src/main/java/com/kumpello/whereiseveryone/main/friends/ui/DeleteFriendDialog.kt)
#### [MODIFY] [Friend.kt](file:///home/kumpel/AndroidStudioProjects/WhereIsEveryone2/app/src/main/java/com/kumpello/whereiseveryone/main/friends/ui/Friend.kt)
#### [MODIFY] [FriendsScreen.kt](file:///home/kumpel/AndroidStudioProjects/WhereIsEveryone2/app/src/main/java/com/kumpello/whereiseveryone/main/friends/ui/FriendsScreen.kt)
#### [MODIFY] [NfcReadingDialog.kt](file:///home/kumpel/AndroidStudioProjects/WhereIsEveryone2/app/src/main/java/com/kumpello/whereiseveryone/main/friends/ui/NfcReadingDialog.kt)
#### [MODIFY] [NfcSharingDialog.kt](file:///home/kumpel/AndroidStudioProjects/WhereIsEveryone2/app/src/main/java/com/kumpello/whereiseveryone/main/friends/ui/NfcSharingDialog.kt)
#### [MODIFY] [QrCodeDialog.kt](file:///home/kumpel/AndroidStudioProjects/WhereIsEveryone2/app/src/main/java/com/kumpello/whereiseveryone/main/friends/ui/QrCodeDialog.kt)
#### [MODIFY] [ShareProfileContent.kt](file:///home/kumpel/AndroidStudioProjects/WhereIsEveryone2/app/src/main/java/com/kumpello/whereiseveryone/main/friends/ui/ShareProfileContent.kt)

### [Common UI]

#### [MODIFY] [Logo.kt](file:///home/kumpel/AndroidStudioProjects/WhereIsEveryone2/app/src/main/java/com/kumpello/whereiseveryone/common/ui/entity/Logo.kt)
- "Where is Everyone!?", "App name"

#### [MODIFY] [FriendDetailsCard.kt](file:///home/kumpel/AndroidStudioProjects/WhereIsEveryone2/app/src/main/java/com/kumpello/whereiseveryone/main/common/ui/FriendDetailsCard.kt)
- "Distance: %s", "Latitude", "Longitude", "Bearing", "Altitude", "Accuracy", "Last Update", "Data Age", "Friend since", "Close", "Navigate!"

## Verification Plan

### Automated Tests
- Build the project to ensure all resource references are correct.
- `gradlew app:assembleDebug`

### Manual Verification
- Verify that the strings are displayed correctly in the app.
