# Focus Block (Android)

Blocks every app except the few you choose, including their internet access, on a schedule and
with a daily time limit. Built with standard Android system components (Accessibility Service +
a local VPN), the same approach real "app blocker" apps use.

**This is a first draft.** I could not compile or test it on a device, so expect to need a round
of fixes after the first build. Send me the exact error from GitHub's Actions log (or from your
phone) and I'll correct it.

## Build the APK for free (no Android Studio needed)
1. Create a free GitHub account and a new repository (private is fine).
2. Upload everything in this folder, keeping the paths exactly as they are — the file
   `.github/workflows/build-apk.yml` must stay at that path. If your upload skips hidden folders,
   use Add file → Create new file, type `.github/workflows/build-apk.yml` as the name, and paste
   its contents.
3. Open the repository's **Actions** tab → **Build APK** → **Run workflow**. Give it 5–10 minutes.
4. Open the finished run → **Artifacts** → download **focus-block-apk**, then unzip it.
5. Copy `app-debug.apk` to your phone, open it, and allow "Install unknown apps" when asked.
   If Play Protect warns you, choose "Install anyway" — this is expected for an app you built
   yourself and have not published.

## Setting it up on your phone, in order
1. Open the app. Under **2. Choose up to 3 apps**, tick the ones you want to allow, then press
   **Save**.
2. Press **Turn on the blocking service (Accessibility)** → find "Focus Block" in the list that
   opens → turn it on. Android will warn that it can see what's on your screen; that's what lets
   it detect and close other apps.
3. Press **Turn on internet blocking (VPN)** → accept the system's VPN prompt. Your phone will
   show a small key or VPN icon in the status bar from then on — that's normal and means it's
   working, not that your data is being sent anywhere.
4. Press **Allow Focus Block to run in the background** so Android doesn't kill it a few minutes
   after you stop looking at your phone.
5. Optionally turn on **3. Allowed hours** to restrict even your allowed apps to certain times,
   and set your **4. Daily limit** in minutes.
6. Turn the **Blocking is on** switch on, and you're set.

## Real limitations — please read before relying on this
- **Not foolproof.** Without root access, no app can make itself impossible to turn off. Turning
  off the Accessibility permission, or force-stopping or uninstalling the app in Settings,
  defeats it — same as with Forest, AppBlock and similar apps. This is meant to raise friction
  and support your intention, not to physically stop you.
- **Background limits vary by phone.** Some brands (Xiaomi, Oppo, Vivo, Samsung among them) kill
  background services aggressively regardless of the battery-exemption setting. If blocking stops
  working after a while, that phone's battery settings are the most likely cause — search your
  phone model plus "don't kill my app" for the specific steps.
- **The VPN icon is expected.** It does not mean your traffic leaves the phone; the tunnel is
  local and simply discards the packets from blocked apps instead of sending them anywhere.
- **Calls, SMS and emergency functions are untouched** — only internet data is affected.
- **No custom icon yet.** It uses a plain system icon for now; I can add one if you'd like.

## If the build fails
Open the failed run in the Actions tab, click the red ✗ step, and send me the last ~30 lines of
its log. Gradle/Android builds are sensitive to exact versions, so the most likely first-round
issues are a dependency version mismatch or an Android SDK license step — both are quick to fix
once I see the real error.
