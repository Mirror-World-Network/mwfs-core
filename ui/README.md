# MW/Sharder Client UI #

## Compile and run in npm
0. run `npm i`
1. run `npm run generate_theme`
2. run `npm run dev`
3. access URL: [http://localhost:4000](http://localhost:4000)

## Build and run in Client
1. run `npm i`
2. run `npm run generate_theme`
3. run `npm run build`
4. startup MW Client service
5. access URL: [http://localhost:7216](http://localhost:7216)

## UI Client Options
MW Client:
```properties
ui/src/styles/css/vars.scss
$projectName: mw

ui/build/config.js
module.exports.title="MW-Client"

ui/static/favicon.ico 
ui/static/img/*
> Replace with the corresponding icon
```

Sharder Client:
```properties
ui/src/styles/css/vars.scss
$projectName: sharder

ui/build/config.js
module.exports.title="Sharder-Client"

ui/static/favicon.ico 
ui/static/img/*
> Replace with the corresponding icon
```
After the configuration is complete, the UI will be automatically switched

## notice
if npm run dev : webpack: Failed to compile.
You can install windows-build-tools 
```cmd
npm install -g windows-build-tools
``` 
in your node environment first, if you still get an error after installation, execute 
```cmd
npm install --save-dev pngquant
``` 
, and finally try to execute 
```cmd
yarn install --ignore-engines
``` 

Another thing to note is that you need to use the compiler to open the ui directory separately


