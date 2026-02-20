# Publishing to Maven Central

Maven Central is the primary repository for Java libraries. This guide explains how to publish AuthLib.

## Prerequisites

1. Create Sonatype account: https://issues.sonatype.org/secure/Signup!default.jspa
2. Create a JIRA ticket for namespace confirmation
3. GPG installed: https://www.gnupg.org/download/
4. Maven installed and configured

## Step 1: Configure Maven Settings

Edit `~/.m2/settings.xml`:

```xml
<servers>
  <server>
    <id>ossrh</id>
    <username>your-username</username>
    <password>your-token</password>
  </server>
</servers>

<profiles>
  <profile>
    <id>ossrh</id>
    <activation>
      <activeByDefault>true</activeByDefault>
    </activation>
    <properties>
      <gpg.executable>gpg</gpg.executable>
      <gpg.passphrase>your-gpg-passphrase</gpg.passphrase>
    </properties>
  </profile>
</profiles>
```

## Step 2: Generate GPG Keys

```bash
gpg --gen-key
gpg --list-keys
gpg --keyserver hkp://pool.sks-keyservers.net --send-keys YOUR_KEY_ID
```

## Step 3: Configure pom.xml

Ensure your `pom.xml` includes:

```xml
<distributionManagement>
  <snapshotRepository>
    <id>ossrh</id>
    <url>https://oss.sonatype.org/content/repositories/snapshots</url>
  </snapshotRepository>
  <repository>
    <id>ossrh</id>
    <url>https://oss.sonatype.org/service/local/staging/deploy/maven2/</url>
  </repository>
</distributionManagement>

<scm>
  <connection>scm:git:https://github.com/authlib/authlib-java.git</connection>
  <developerConnection>scm:git:https://github.com/authlib/authlib-java.git</developerConnection>
  <url>https://github.com/authlib/authlib-java</url>
</scm>

<license>
  <name>MIT</name>
  <url>https://opensource.org/licenses/MIT</url>
</license>

<developers>
  <developer>
    <id>authlib</id>
    <name>AuthLib Contributors</name>
    <email>support@authlib.dev</email>
  </developer>
</developers>
```

## Step 4: Build and Deploy

### Deploy to Staging Repository

```bash
mvn clean deploy -P ossrh
```

### Release from Staging

Log in to: https://oss.sonatype.org/#stagingRepositories

1. Find your staging repository
2. Click "Close" button
3. Click "Release" button
4. Wait for sync to Maven Central (can take 30 minutes)

### Or use Maven plugin

```bash
mvn nexus-staging:release
```

## Step 5: Verify Publication

After about 30 minutes:

```bash
mvn dependency:get -Dartifact=dev.authlib:authlib:1.0.0
```

Check: https://search.maven.org/search?q=authlib

## Usage After Publishing

Users can add to their `pom.xml`:

```xml
<dependency>
  <groupId>dev.authlib</groupId>
  <artifactId>authlib</artifactId>
  <version>1.0.0</version>
</dependency>
```

Or Gradle:

```gradle
implementation 'dev.authlib:authlib:1.0.0'
```

## Troubleshooting

- **Deployment fails**: Check Maven settings and credentials
- **GPG errors**: Ensure GPG key is properly generated and exported
- **Repository rules**: Ensure all files (javadoc, sources) are present
- **Timeout**: Sonatype deployment can be slow, be patient

## Best Practices

- Use semantic versioning
- Include comprehensive documentation
- Provide source code and Javadoc
- Sign all artifacts with GPG
- Keep releases consistent
- Monitor Maven Central for sync issues
