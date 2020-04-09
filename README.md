# HotFix
这是1.0版本，只写了类加载。后续会加上底层替换、Instant Run，资源修复和so库修复


##基于类加载的修复原理

1、首先Android的类加载也是基于双亲委托机制，一个类只会被加载一次。那么我可以把有bug的类打包成补丁dex，下发到手机中，
然后在加载原来的bug类之前加载补丁dex中修复好的类，那以后就不会再加载原来的bug类了，这种思路就可以到达修复class文件bug的目的。

2、思路很明确了，打包补丁dex也很容易（具体操作后面会说），下发补丁到app也容易。那就只剩下一个问题了，如何让已经修复好的类先于有bug的类加载？

查找class是通过DexPathList来完成的，内部是DexPathList遍历Element数组，通过Element获取DexFile对象来加载Class文件。
由于数组是有序的，如果2个dex文件中存在相同类名的class，那么类加载器就只会加载数组前面的dex中的class。如果apk中出现了有bug的class，
那只要把修复的class打包成dex文件并且放在DexPathList中Element数组的前面，就可以实现bug修复了。
