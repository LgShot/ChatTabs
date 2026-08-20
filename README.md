# ChatTabs 3.0 — Multi-task experiment

ChatTabs 3.0 tests a different architecture: **one saved slot = one Android document task**.
It does not use a browser, WebView, Accessibility, or conversation-title automation.

## Intended behavior

1. Create slots such as `HTB`, `SOC`, `B1`, `Kültür`.
2. Open a slot for the first time. ChatTabs creates a unique task with base URI `chattabs://slot/<id>` and launches the official `com.openai.chatgpt` activity on top of it **without** `FLAG_ACTIVITY_NEW_TASK`.
3. In that ChatGPT task, manually open the conversation you want and leave it there.
4. Open another slot and choose another conversation.
5. Reopening the original slot moves its existing task to the foreground rather than starting a new one.

This depends on the official ChatGPT launch activity accepting normal Android task semantics. If ChatGPT declares or enforces `singleTask`/`singleInstance` behavior, Android can collapse the instances back to one task. In that case this experiment proves that a clone/container approach is required.

## Extras

- Per-slot pinned home-screen shortcuts (`requestPinShortcut`).
- First four slots also become dynamic launcher shortcuts.
- First six slots appear in the ChatTabs widget.
- `Açık ChatTabs tasklarını sıfırla` removes only ChatTabs-created slot tasks from Recents.
- No `INTERNET` permission.

## Stable test signing

Starting with v3, the repository carries a fixed non-production debug keystore so later ChatTabs test APKs can update v3 without changing Android signatures. Upgrading from older v1/v2 GitHub-runner debug builds may still require uninstalling the old ChatTabs once.
