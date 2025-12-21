from appium import webdriver
from appium.webdriver.common.appiumby import AppiumBy
import time
import pandas as pd

#setting up the capabilities
caps = {
    "platformName": "Android",
    "automationName": "UiAutomator2",
    "deviceName": "emulator-5554",
    "appPackage": "ca.razroze.doro.app", "xyz.slingshot.ashley.app"
    "appActivity": ".MainActivity",
    "noReset": True
}
