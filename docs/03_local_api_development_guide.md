# Local Development Setup for OSDU Core Lib Azure

This guide explains how to set up and use the local development authentication features in the OSDU Core Lib Azure library.

## Overview

The OSDU Core Lib Azure library now supports local development authentication using familiar developer tools like Azure CLI, VS Code, and IntelliJ. This eliminates the need to manage service principal credentials locally while maintaining security for production deployments.

## Prerequisites

Before using local development authentication, ensure you have one of the following tools installed and configured:

### Option 1: Azure CLI (Recommended)
1. Install [Azure CLI](https://docs.microsoft.com/en-us/cli/azure/install-azure-cli)
2. Login to your Azure account:
   ```bash
   az login
   ```
3. Set your subscription (if you have multiple):
   ```bash
   az account set --subscription <your-subscription-id>
   ```

### Option 2: Visual Studio Code
1. Install [Visual Studio Code](https://code.visualstudio.com/)
2. Install the [Azure Account extension](https://marketplace.visualstudio.com/items?itemName=ms-vscode.azure-account)
3. Sign in to Azure through VS Code (Ctrl/Cmd+Shift+P → "Azure: Sign In")

### Option 3: IntelliJ IDEA
1. Install [IntelliJ IDEA](https://www.jetbrains.com/idea/)
2. Install the [Azure Toolkit for IntelliJ](https://plugins.jetbrains.com/plugin/8053-azure-toolkit-for-intellij)
3. Sign in to Azure through IntelliJ

## Using the Local Development Library

### Step 1: Build and Install the Library Locally

First, build and install the library with local development support in your local Maven repository:

```bash
cd /path/to/os-core-lib-azure-community
mvn clean install -DskipTests
```

### Step 2: Update Your Consuming Service

Update your service's `pom.xml` to use the locally built version:

```xml
<dependency>
    <groupId>org.opengroup.osdu</groupId>
    <artifactId>core-lib-azure</artifactId>
    <version>development-SNAPSHOT</version>
</dependency>
```

### Step 3: Configure Local Development Profile

Create or update your `application-local.properties` file in your consuming service:

```properties
# Enable local development profile
spring.profiles.active=local

# Local development settings
azure.local.development.enableLocalCredentials=true

# Disable production authentication methods
azure.paas.workloadidentity.isEnabled=false
azure.paas.podidentity.isEnabled=false

# Optional: Disable production-only features
azure.pubsub.publish=false
azure.eventgrid.manager.enabled=false
```

### Step 4: Run Your Service Locally

Run your service with the local profile active:

```bash
# Option 1: Using environment variable
export SPRING_PROFILES_ACTIVE=local
mvn spring-boot:run

# Option 2: Using command line argument
mvn spring-boot:run -Dspring.profiles.active=local

# Option 3: Using IDE run configuration
# Set VM options: -Dspring.profiles.active=local
```

## How It Works

When the local profile is active, the authentication flow works as follows:

1. **Local Development Check**: The system detects that the `local` profile is active
2. **Credential Chain**: Uses Azure Identity SDK's credential chain in this order:
   - Azure CLI credential (most common)
   - Visual Studio Code credential
   - IntelliJ credential
3. **Fallback**: If local development is disabled, falls back to production methods:
   - Workload Identity (for AKS)
   - Managed Identity (for Azure VMs)
   - Service Principal (with client credentials)

## Security Features

### Environment Validation

The library includes safety checks to prevent accidental use in production:

- **Kubernetes Detection**: Throws error if running in Kubernetes with local profile
- **Azure App Service Detection**: Throws error if running in Azure App Service
- **Azure Container Instance Detection**: Throws error if running in ACI
- **Managed Identity Detection**: Throws error if Azure Managed Identity is available

### Warning Logs

When local development mode is active, you'll see prominent warning messages:

```
==========================================================
LOCAL DEVELOPMENT PROFILE IS ACTIVE
Using local development credentials (Azure CLI/VS Code/IntelliJ)
This configuration should NEVER be used in production!
==========================================================
```

## Switching Between Versions

### To Use Local Development Version:
First build and install locally:
```bash
cd /path/to/os-core-lib-azure-community
mvn clean install -DskipTests
```

Then use in your project:
```xml
<dependency>
    <groupId>org.opengroup.osdu</groupId>
    <artifactId>core-lib-azure</artifactId>
    <version>development-SNAPSHOT</version>
</dependency>
```

Run with: `mvn spring-boot:run -Dspring.profiles.active=local`

### To Switch Back to Production Version:
```xml
<dependency>
    <groupId>org.opengroup.osdu</groupId>
    <artifactId>core-lib-azure</artifactId>
    <version>YOUR_PRODUCTION_VERSION</version>
</dependency>
```

Run normally without the local profile.

## Troubleshooting

### Common Issues

#### 1. "No credentials available" error
**Solution**: Ensure you're logged into Azure via Azure CLI, VS Code, or IntelliJ:
```bash
az login
az account show  # Verify you're logged in
```

#### 2. "Security violation" error
**Cause**: Local profile detected in production environment
**Solution**: Remove the local profile or check environment variables

#### 3. Token acquisition fails
**Solutions**:
- Verify your Azure permissions for the target resources
- Check if your account has the necessary role assignments
- Try refreshing your credentials: `az logout && az login`

#### 4. Dependencies not resolving
**Solution**: Ensure the local library is installed:
```bash
cd /path/to/os-core-lib-azure-community
mvn clean install -DskipTests
```

### Debug Information

To see detailed authentication flow:
```bash
export AZURE_LOG_LEVEL=VERBOSE
mvn spring-boot:run -Dspring.profiles.active=local
```

## Best Practices

1. **Never commit local profile configuration to production branches**
2. **Use environment-specific configuration files**:
   - `application.properties` (production)
   - `application-local.properties` (local development)
   - `application-dev.properties` (development environment)
3. **Regularly refresh your Azure credentials**: `az login`
4. **Use least-privilege access** - only grant necessary permissions to your account
5. **Keep the library version separate** between local and production environments

## Configuration Reference

### Available Properties

```properties
# Local Development Configuration
azure.local.development.enableLocalCredentials=true/false

# Production Authentication Configuration
azure.paas.workloadidentity.isEnabled=true/false
azure.paas.podidentity.isEnabled=true/false

# Feature Toggles
azure.pubsub.publish=true/false
azure.eventgrid.manager.enabled=true/false
azure.serviceBus.manager.enabled=true/false
```

### Spring Profiles

- `local`: Enables local development authentication
- `dev`: Development environment (customize as needed)
- `prod`: Production environment (default behavior)

## Support

For issues or questions related to local development setup:

1. Check the troubleshooting section above
2. Verify your Azure account permissions
3. Ensure the library version matches your requirements
4. Review application logs for detailed error messages

The local development features are designed to provide a seamless development experience while maintaining security boundaries between local and production environments.
