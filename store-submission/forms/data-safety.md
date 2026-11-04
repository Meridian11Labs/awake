# Data safety (Play Console › Policy › App content › Data safety)

| Question | Answer |
|---|---|
| Does your app collect or share any of the required user data types? | **No** |
| Is all of the user data collected by your app encrypted in transit? | (not asked when nothing is collected) |
| Do you provide a way for users to request that their data is deleted? | (not asked when nothing is collected) |

Why: Awake has no `INTERNET` permission, no SDKs that send data, and no accounts. Its settings are kept in its
private app storage (DataStore) on the device. Android's own backup may include them in the user's Google account;
Play's guidance treats that as not collected by the app.
