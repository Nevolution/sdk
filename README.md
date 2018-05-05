<a href='https://play.google.com/store/apps/details?id=com.oasisfeng.nevo&referrer=utm_source%3Dgithub%26utm_medium%3Dreadme%26utm_content%3Dbadge'><img height='20%' width='20%' alt='Get it on Google Play' src='https://play.google.com/intl/en_us/badges/images/generic/en_badge_web_generic.png'/></a>

About
-------

Nevolution is an open platform for Android to evolve the notification experience of existing apps, in a creative **developer-independent** way via community-driven plug-ins, without the direct involvement of the original app developer.


Background
------------

Android supports a highly rich notification experience, and the feature set has expanded in almost every major Android version. Unfortunately, most apps in the wild only use a small set of the capabilities in their notifications, rendering them less elegant, functionality lacking and even ugly on your modern Android devices.

If you use an Google Wear OS smart watch or Android Auto powered car, it's extremely disappointing that many of your favorite apps do not provide friendly integration with them.

The Nevolution community is proactively changing the philosophy behind notifications and aims to set an example of great user experience.


Developer Community
---------------------

Nevolution is currently in beta stage and open to public. Since the end-user experience largely depends on the richness of 3rd-party plug-ins, we sincerely welcome developers to join our developer community, to build various generic or app-specific plug-ins and a more powerful Nevolution platform together.

Developing a simple plug-in for Nevolution with our developer-friendly SDK is even easier than writing a tiny app. With a creative idea in your mind, just one or two hours of coding is enough to build a working plug-in. The code of [sample plug-ins](https://github.com/search?q=topic%3Anevolution-decorator+org%3ANevolution) already shows how easy writing a plug-in could be.

We are devoted to working with the developer community to build high quality plug-ins and improve Nevolution. So developers are the most important roles to build a healthy community.

Discussion goes to the [Telegram Group (for developers)](https://t.me/joinchat/E-QUwkmvU2dlVkivIZBYEQ), while issues and formal feature request should be posted on the GitHub for better tracking.

Developers are suggested to join the [BETA CHANNEL](https://play.google.com/apps/testing/com.oasisfeng.nevo) for the fast iteration on new features and APIs.


How Does Nevolution Work
--------------------------

Nevolution consists of one platform app and many third party plug-in apps. A plug-in is not necessarily just a single-purpose app, it can bundle several plug-ins, or be part of a regular app.

Currently the only supported plug-in type is **"decorator"**, which receives notification and makes necessary tweaks on the fly. All decorators enabled by user work together in a pipeline to decorate any incoming notification.

A decorator plug-in is essentially a regular exported Android service component declared with an intent-filter recognized by Nevolution platform.

Nevolution does not require *root*, but root-capable features are being considered.


Get Started
-------------

Clone any of the [sample decorator repos](https://github.com/search?q=topic%3Anevolution-decorator+org%3ANevolution) to start developing new plug-in for Nevolution, or fork this repo to contribute to the SDK. And yes, the official plug-ins hosted in this organization are also open for contributions.

Build and install your plug-in app on the device, Nevolution will recognize it and let you activate it for the selected app right away.

**Head to wiki page for more details: [Decorator Plug-in Development](https://github.com/Nevolution/sdk/wiki/Decorator-Plugin-Development)**
