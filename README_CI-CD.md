# Study Room: CI and CD with GitHub Actions

This project is the room-booking code from the API lab, now with a **build, a test suite and a GitHub Actions pipeline** (SENG 300 T12: CI and CD). The API lab itself is unchanged: see [README_API.md](README_API.md).

## The idea

- **CI (continuous integration)**: every push to `main` automatically builds the code and runs the tests on a clean machine. A broken change is noticed in minutes, not at demo time.
- **CD (continuous delivery/deployment)**: when the tests pass, the pipeline packages the program (a jar file) and saves it. Nobody builds it by hand.

```
git push to main
      │
      ▼
GitHub reads .github/workflows/ci.yml
      │
      ▼
┌─────────────┐  pass   ┌──────────────────────────┐
│ test job    │ ──────► │ deploy job               │
│ ./gradlew   │         │ ./gradlew jar            │
│   test      │         │ java -jar ...jar         │
└─────────────┘         │ upload jar as "study-room"│
      │ fail            └──────────────────────────┘
      ▼
deploy is skipped, no jar
```

## Run it on your laptop first

The pipeline runs the same commands you run locally. Needs JDK 17 or newer (CI uses 21).

```sh
./gradlew test                              # run the tests
./gradlew jar                               # build build/libs/study-room-0.1.0.jar
java -jar build/libs/study-room-0.1.0.jar   # runs Step1_LibraryApi, prints Booking{id=R-1,...}
```

## The workflow file, line by line

`.github/workflows/ci.yml`:

```yaml
name: study-room          # name shown on the Actions tab

on:
  push:
    branches: [main]      # a push to main starts the workflow
```

**The `test` job** runs on a clean Ubuntu machine (a *runner*), not your laptop. Its steps run in order:

```yaml
jobs:
  test:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4          # 1. get this commit
      - uses: actions/setup-java@v4        # 2. install Java 21
        with:
          distribution: temurin
          java-version: 21
      - run: ./gradlew test                # 3. same command as on the laptop
```

**The `deploy` job** starts only after `test` passes:

```yaml
  deploy:
    needs: test                            # waits for test; skipped if test fails
    runs-on: ubuntu-latest                 # a NEW machine: checkout and Java again
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-java@v4
        with: { distribution: temurin, java-version: 21 }
      - run: ./gradlew jar                 # build the jar
      - run: java -jar build/libs/study-room-0.1.0.jar   # run it; the log shows the booking
      - uses: actions/upload-artifact@v4   # save the jar on the run
        with:
          name: study-room
          path: build/libs/*.jar
```

## Hands-on

### 1. Put the project on GitHub

Create an empty repo on GitHub, then from this folder:

```sh
git init
git add .
git commit -m "Add the study-room project and the GitHub Actions workflow."
git branch -M main
git remote add origin git@github.com:YOUR-ACCOUNT/YOUR-REPO.git
git push -u origin main
```

Open the repo's **Actions** tab. The push started a run: `test` (green), then `deploy` (green).

### 2. Make it fail

In `src/test/java/RoomServiceTest.java`, change the expected status so it no longer matches what the code returns:

```java
assertThat(b.status).isEqualTo("pending");   // the code returns "confirmed"
```

```sh
git add src/test/java/RoomServiceTest.java
git commit -m "Expect pending so the test fails."
git push
```

On the Actions tab the `test` job is red and `deploy` is **skipped**: no jar is built. Open the failed run and read the log: it names the failing test, the expected value and the actual value.

### 3. Make it pass

Put `"confirmed"` back.

```sh
git add src/test/java/RoomServiceTest.java
git commit -m "Expect confirmed again."
git push
```

Both jobs go green. In the `deploy` log you will see the program's output (`Got back a Java object: Booking{id=R-1, room=MS-101, status=confirmed}`).

### 4. Download the jar

Open the successful run. Under **Artifacts** at the bottom there is a download named `study-room`. That is the jar the pipeline built.

## Reading a failed run

1. Click the red run, then the failed job (`test`).
2. Expand the failed step (`Run ./gradlew test`).
3. Find the line with `FAILED` and the assertion message (`Expecting ... to be equal to ...`).
4. Reproduce it locally with `./gradlew test`, fix it, push again.

## Notes

- The jar is built with `jar { manifest { attributes 'Main-Class': 'Step1_LibraryApi' } }` in `build.gradle`; that is what makes `java -jar` work.
- If `./gradlew` says permission denied on macOS/Linux: `chmod +x gradlew`, then `git add gradlew` again.
- The API lab's commands now compile from `src/main/java` (`javac -d out src/main/java/*.java`); `demo.sh` was updated the same way.
