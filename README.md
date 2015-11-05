[![Join the chat at https://gitter.im/oasisfeng/nevolution](https://badges.gitter.im/Join%20Chat.svg)](https://gitter.im/oasisfeng/nevolution?utm_source=badge&utm_medium=badge&utm_campaign=pr-badge&utm_content=badge)
[![Build Status](https://travis-ci.org/oasisfeng/nevolution.svg?branch=master)](https://travis-ci.org/oasisfeng/nevolution)
[![License: Apache 2.0](https://img.shields.io/badge/license-Apache%202.0-blue.svg)](https://github.com/oasisfeng/nevolution)

About
-------

Nevolution is an open platform for Android to evolve the notification experience of existing apps, in a creative **developer-independent** way via community-driven plug-ins, without the direct involvement of the original app developer.


Background
------------

Android supports highly rich notification experience, and the feature set expanded in almost every major Android versions. Unfortunately, most apps in the wild only use a small set of limited capabilities in their notifications, rendering them less elegant, functionality lacking and even ugly on your modern Android devices.

If you use Android Wear smart watch or Android Auto powered car, it's extremely disappointing that probably most of your favorite apps do not work on them.

We are not asking any more, but teaching them the right way to build better experience, with the efforts from Nevolution community.


Developer Preview
-------------------

Nevolution is currently under active development. Since the end-user experience largely depends on the richness of 3rd-party plug-ins, we decided to release the developer preview version in beta test before the public release.

Developing a simple plug-in for Nevolution with our developer-friendly SDK is even easier than writing a tiny app. With a creative idea in your mind, just one or two hours of coding is enough to build a working plug-in. The code of built-in plug-ins already shows how simple a plug-in could be.

[Join the beta test](https://play.google.com/apps/testing/com.oasisfeng.nevo) first, then [install it on Google Play](https://play.google.com/store/apps/details?id=com.oasisfeng.nevo). There might be a short delay before the latter link could work.

We are devoted to work with the developer community to build high quality plug-ins and improve Nevolution. So developers are the most important roles to build a healthy community.

Discussion goes to [our G+ community](https://plus.google.com/communities/108874686073587920040) or [Gitter chat](https://gitter.im/oasisfeng/nevolution?utm_source=readme&utm_medium=link&utm_campaign=dev-preview&utm_content=discussion), while issues and formal feature request should be posted on the GitHub for better tracking.


How Does Nevolution Work
--------------------------

Nevolution consists of one platform app and many plug-in apps, developed by different developers. Plug-in is not necessarily just a single-purpose app, it can bundle several plug-ins, or be part of an regular app.

Currently the only supported plug-in type is "decorator", which receives notification and makes necessary modifications on the fly. All decorators enabled by user works together in a pipeline to decorate any incoming notification.

A decorator plug-in is essentially a regular exported Android service component declared with an intent-filter recognized by Nevolution platform. See the code of decorators in this repository for details.

Nevolution does not require *root*, but root-capable features are planned to be added in the near future.


Get Started
-------------

Clone this repository to start developing new plug-in for Nevolution, or fork it to contribute to the built-in plug-ins and SDK.

Build and install your plug-in app on the device, Nevolution will recognize it and let you activate it for the selected app right away.

Head to wiki page for more details: [Decorator Plug-in Development](https://github.com/oasisfeng/nevolution/wiki/Decorator-Plugin-Development)


License
---------
The source code and related materials of the SDK and built-in decorators are licensed under Apache License 2.0.
