# Liftrr Setup Guide

This document explains how to set up the Liftrr project for local development and CI/CD.

## Prerequisites

- Android Studio Hedgehog or newer
- JDK 17 or newer
- Android SDK (API level 34+)
- Git

## Local Development Setup

### 1. Clone the Repository

```bash
git clone https://github.com/faizanmir/Liftrr-app.git
cd Liftrr-app
```

### 2. Configure Secrets

The app requires OAuth credentials for Google Sign-In. These are stored in `secrets.xml` which is **not** committed to Git for security.

#### Option A: Using the Template (Recommended)

```bash
cp app/secrets.xml.template app/src/main/res/values/secrets.xml
```

Then edit `app/src/main/res/values/secrets.xml` and replace `YOUR_GOOGLE_WEB_CLIENT_ID` with your actual Web Client ID.

#### Option B: Create from Scratch

Create `app/src/main/res/values/secrets.xml`:

```xml
<resources>
    <string name="web_client_id" translatable="false">YOUR_WEB_CLIENT_ID_HERE</string>
</resources>
```

#### Getting Your Google Web Client ID

1. Go to [Google Cloud Console](https://console.cloud.google.com/apis/credentials)
2. Create or select your project
3. Navigate to **APIs & Services > Credentials**
4. Find your **OAuth 2.0 Client IDs**
5. Copy the **Web application** client ID (not the Android client ID)

### 3. Configure google-services.json

Download your `google-services.json` from Firebase Console and place it in `app/`:

```bash
# Place your google-services.json here:
app/google-services.json
```

**Note:** This file is also in `.gitignore` and should not be committed.

### 4. Build the Project

```bash
./gradlew assembleDebug
```

## CI/CD Setup (GitHub Actions)

For automated builds on GitHub, you need to configure secrets as GitHub repository secrets.

### Required GitHub Secrets

1. **GOOGLE_WEB_CLIENT_ID**: Your Google OAuth Web Client ID
2. **GOOGLE_SERVICES_JSON**: Base64-encoded contents of `google-services.json`

### Setting Up GitHub Secrets

#### 1. Add Web Client ID

Go to your repository **Settings > Secrets and variables > Actions > New repository secret**:

- Name: `GOOGLE_WEB_CLIENT_ID`
- Value: Your web client ID (e.g., `123456-abc.apps.googleusercontent.com`)

#### 2. Add google-services.json

First, encode your file to base64:

```bash
# On macOS/Linux:
base64 -i app/google-services.json | pbcopy

# On Windows (PowerShell):
[Convert]::ToBase64String([IO.File]::ReadAllBytes("app\google-services.json")) | clip
```

Then add as a secret:

- Name: `GOOGLE_SERVICES_JSON`
- Value: Paste the base64-encoded content

### GitHub Actions Workflow

Your workflow should decode and create these files before building:

```yaml
- name: Create secrets.xml
  run: |
    mkdir -p app/src/main/res/values
    cat > app/src/main/res/values/secrets.xml <<EOF
    <resources>
        <string name="web_client_id" translatable="false">${{ secrets.GOOGLE_WEB_CLIENT_ID }}</string>
    </resources>
    EOF

- name: Create google-services.json
  run: |
    echo "${{ secrets.GOOGLE_SERVICES_JSON }}" | base64 -d > app/google-services.json

- name: Build app
  run: ./gradlew assembleDebug
```

## Security Best Practices

### ✅ DO:
- Keep `secrets.xml` and `google-services.json` in `.gitignore`
- Use GitHub Secrets for CI/CD
- Rotate credentials if accidentally committed
- Use different credentials for debug/release builds

### ❌ DON'T:
- Commit `secrets.xml` to Git
- Share credentials in chat/email
- Use production credentials in debug builds
- Hardcode secrets in source code

## Troubleshooting

### Build fails with "web_client_id not found"

**Solution:** You haven't created `secrets.xml`. Follow step 2 above.

### Google Sign-In fails with "Invalid client"

**Possible causes:**
1. Wrong Web Client ID in `secrets.xml`
2. Using Android Client ID instead of Web Client ID
3. OAuth consent screen not configured in Google Cloud Console

**Solution:** Verify you're using the **Web application** OAuth client ID.

### CI/CD build fails on GitHub

**Solution:** Make sure you've added both `GOOGLE_WEB_CLIENT_ID` and `GOOGLE_SERVICES_JSON` as repository secrets.

## Additional Resources

- [Google Sign-In for Android](https://developers.google.com/identity/sign-in/android/start)
- [Firebase Console](https://console.firebase.google.com/)
- [Google Cloud Console](https://console.cloud.google.com/)

## Support

For issues, please open a GitHub issue with:
- Android Studio version
- Error messages or logs
- Steps to reproduce
