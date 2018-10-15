Say we have a library that looks like this:

```js
module pdf {
    public exported function parse(inputFile) {
        // ...
        if (inputFile.contains(OBSCURE_FONT_FORMAT)) {
            loadObscureFontsFrom(inputFile);
        }
        // ...
    }
    
    private function loadObscureFontsFrom(file) {
        // boom!
    }
}
```

`loadObscureFontsFrom` actually has the exploitable vulnerability.

If `parse` is ever called on user input, the application is vulnerable.

Declaring `loadObscureFontsFrom` as the vulnerable method doesn't give you
any useful information: it will never be called, unless you are under attack.
We will tell people that this vulnerability isn't worth fixing, and we're wrong.

Declaring `parse` as the vulnerable method isn't very accurate, and will almost
always be called in any application. We would need to show that it's called on
user input, which is very hard.

Which are we adding to the database?

 * the patch/commit changes only code in `loadObscureFontsFrom`
