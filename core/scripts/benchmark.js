#!/usr/bin/env node

const puppeteer = require('puppeteer');

(async function run() {

  console.log("START TESTS")

  const browser = await puppeteer.launch({headless: "new"});
  const page = await browser.newPage();

  page.on("pageerror", console.error);

  page.on("console", async (m) => {
    if (m.type() === "error") {
      console.error(`${m.text()} in ${m.location().url}:${m.location().lineNumber}`);
    } else {
      const values = await Promise.all(m.args().map(h => h.jsonValue()));
      console.log(...values);
    }
  });

  await page.exposeFunction("testsDone", async ([react, uix, reagent]) => {
    console.log(
      `
React ${react}ms
UIx ${uix}ms ${Math.round(((100 / react * uix) / 100) * 10) / 10}x
Reagent ${reagent}ms ${Math.round(((100 / react * reagent) / 100) * 10) / 10}x`);

      await browser.close();
    }
  );

  console.log("RUNNING TESTS");

  await page.goto(`file://${process.argv[2]}/index.html`);

  console.log("BROWSER OPENED");
})();

setTimeout(() => {
  process.exit(1);
}, 30000);
