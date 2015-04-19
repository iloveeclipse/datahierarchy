# datahierarchy
Originally hosted on code.google.com/p/datahierarchy

The question I very often have is: which objects (recursively) are loaded in memory if a particular object is created?

If you know Call Hierarchy plugin, you will find the Data Hierarchy very similar. It searches selected Java classes for declared fields, and starting with the found "Data" it recursively looks for fields which are declared on found classes ("Hierarchy").

At the end you can see kind of "Data Hierarchy" tree.

The plugin can not detect reflection based data, data inside non-generic collections, data which is stored as "Object" etc.

This is of course a very rough estimation of the real data hierarchy, but works perfectly for a quick walk over data structures/dependencies during code reviews.

Additionally plugin allows to restrict the search for static fields only.

[Home page](http://andrei.gmxhome.de/datahierarchy/index.html)

You can install the plugin from [Eclipse Marketplace](https://marketplace.eclipse.org/content/data-hierarchy) or download from [bintray](https://bintray.com/iloveeclipse/plugins/DataHierarchy/view/files).
