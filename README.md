About
-------

Nevolution is an open platform for Android to evolve the notification experience of existing apps, via various plug-ins built by developer community.


Background
------------

Android supports highly rich notification experience, and the feature set expanded in almost every major Android versions.
Unfortunately, most Android apps only used very limited capabilities of notification, render them less elegant and even ugly on your modern Android devices.

Nevolution evolves the notifications in a creative developer-independent way, involving community-driven development to improve the 1st-party experience.

We are not asking the app developers for a better experience any more, but teaches them the right way to build a better experience, with the help from Nevolution community. 


Developer Preview
-------------------

Developing a simple plug-in for Nevolution with our easy-to-use SDK is even easier than writing a tiny app.
With a creative idea in your mind, just one or two hours of coding is enough to build a working plug-in.

Nevolution is currently under active development. Since the end-user experience largely depends on the richness of 3rd-party plug-ins,
we released developer preview version in beta test, open for all developers.

We are devoted to work with the developer community to build high quality plug-ins.

Join the [beta test](https://play.google.com/testing/com.oasisfeng.nevo) and install it on Google Play: https://play.google.com/store/apps/details?id=com.oasisfeng.nevo

Discussion goes to [our G+ community](https://plus.google.com/communities/108874686073587920040), while issues and formal feature request should be posted on the GitHub for better tracking.


How Does Nevolution Work
--------------------------

Nevolution consists of one platform app and many plug-in apps, developed by different developers.
Plug-in is not necessarily just a single-purpose app, it can bundle several plug-ins, or be part of an regular app.

Currently the only supported plug-in type is "decorator", which receives notification and makes necessary modifications.
All decorators enabled by user works together in a pipeline to decorate any incoming notifications.

A decorator plug-in is essentially a regular exported Android service component declared with an intent-filter recognized by Nevolution platform.
See the "decorators" module in this repository for details.

Nevolution does not require *root*, but root-capable features are planned to be added.


Get Started
-------------

Clone this repository to start developing new plug-in for Nevolution, or contribute to the built-in plug-ins.

More documents for plug-in development is on the way. Please refer to the JavaDoc and sample code for now.
