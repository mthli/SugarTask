SugarTask
===

Android lifecycle safety task with sugar code style.

Support Android API 14 and UP.

If you have any question or want to discuss, just open an issue. Pull request is always welcome :)

## Gradle

At your top-level `build.gradle` file:

```groovy
repositories {
    // ...
    maven { url "https://jitpack.io" }
}
```

And then at your project `build.gradle` file:

```groovy
dependencies {
    compile 'com.github.mthli:SugarTask:v0.1'
}
```

Done!

## Usage

At your MainThread(UIThread), start a background thread just like this:

```java
SugarTask.with(this) // Activity|FragmentActivity(v4)|Fragment|Fragment(v4)
        .assign(new SugarTask.TaskDescription() {
            @Override
            public Object onBackground() {
                // Do what you want to do on background thread.
                // If you want to post something to MainThread,
                // just call SugarTask.post(YOUR_MESSAGE).

                // Return your finally result(Nullable).
                return null;
            }
        })
        .handle(new SugarTask.MessageListener() {
            @Override
            public void handleMessage(@NonNull Message message) {
                // Receive message in MainThread which sent from WorkerThread,
                // update your UI just in time.
            }
        })
        .finish(new SugarTask.FinishListener() {
            @Override
            public void onFinish(@Nullable Object result) {
                // If WorkerThread finish without Exception and lifecycle safety,
                // deal with your WorkerThread result at here.
            }
        })
        .broken(new SugarTask.BrokenListener() {
            @Override
            public void onBroken(@NonNull Exception e) {
                // If WorkerThread finish with Exception and lifecycle safety,
                // deal with Exception at here.
            }
        })
        .execute();
```

Your don't need to conside about Activity/Fragment lifecycle, no matter screen rotating or some others.

Really nice for you :)

And [here is a simple example](https://github.com/mthli/SugarTask/blob/master/app/src/main/java/io/github/mthli/sugartaskdemo/MainFragment.java "SugarTaskDemo.MainFragment") for your.

__Notice__:

 - `.with()`, `.assign()`, `.execute()` is __MUST__.

 - `.handle()`, `.finish()`, `broken()` is __OPTION__. Every method just call once, otherwise the newer with replace the older.

 - Use `SugarTask.post()` To send message from WorkerThread to MainThread just in time.

## Theory

How to get Activity/Fragment lifecycle state real-time?

It's easy, just add a hook fragment to Activity/Fragment by their FragmentManager, the hook fragment will follow it's parent lifecycle, so we get state real-time :)

When Activity/Fragment is `onStop()`, we just cancel all MainThread callback, so that avoid OOM/NPE.

Get more information from [our source code](https://github.com/mthli/SugarTask/blob/master/lib/src/main/java/io/github/mthli/sugartask/SugarTask.java "SugarTask.java").

## More

SugarTask is so simple that it just works good for easy task, if you need more functions, just have a look at [RxAndroid](https://github.com/ReactiveX/RxAndroid "RxAndroid").

## Thanks

 - [Glide](https://github.com/bumptech/glide "Glide")

## License

    Copyright 2015 Matthew Lee

    Licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at

        http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.
