package com.mauriciotogneri.greencoffee;

import android.support.annotation.IdRes;
import android.support.annotation.StringRes;
import android.support.test.filters.LargeTest;
import android.text.TextUtils;

import com.mauriciotogneri.greencoffee.annotations.Given;
import com.mauriciotogneri.greencoffee.annotations.Then;
import com.mauriciotogneri.greencoffee.annotations.When;

import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.action.ViewActions.click;
import static android.support.test.espresso.action.ViewActions.typeText;
import static android.support.test.espresso.assertion.ViewAssertions.matches;
import static android.support.test.espresso.matcher.ViewMatchers.isDisplayed;
import static android.support.test.espresso.matcher.ViewMatchers.withId;
import static android.support.test.espresso.matcher.ViewMatchers.withText;
import gherkin.AstBuilder;
import gherkin.Parser;
import gherkin.ast.Feature;
import gherkin.ast.GherkinDocument;
import gherkin.ast.ScenarioDefinition;
import gherkin.ast.Step;

@RunWith(Parameterized.class)
@LargeTest
public class GreenCoffeeTest
{
    protected void start(String featureSource, Object target, ControlledActivityTestRule<?> activityTestRule)
    {
        try
        {
            List<StepDefinition> stepDefinitions = stepDefinitions(target);
            Parser<GherkinDocument> parser = new Parser<>(new AstBuilder());
            GherkinDocument gherkinDocument = parser.parse(featureSource);
            processFeature(gherkinDocument.getFeature(), stepDefinitions, activityTestRule);
        }
        catch (Exception e)
        {
            throw new RuntimeException(e);
        }
    }

    private List<StepDefinition> stepDefinitions(Object target)
    {
        List<StepDefinition> stepDefinitions = new ArrayList<>();

        for (Method method : target.getClass().getDeclaredMethods())
        {
            String expression = expression(method);

            if (expression != null)
            {
                StepDefinition stepDefinition = new StepDefinition(expression, method, target);
                stepDefinitions.add(stepDefinition);
            }
        }

        return stepDefinitions;
    }

    private String expression(Method method)
    {
        Given given = method.getAnnotation(Given.class);

        if (given != null)
        {
            return given.value();
        }

        When when = method.getAnnotation(When.class);

        if (when != null)
        {
            return when.value();
        }

        Then then = method.getAnnotation(Then.class);

        if (then != null)
        {
            return then.value();
        }

        return null;
    }

    private void processFeature(Feature feature, List<StepDefinition> stepDefinitions, ControlledActivityTestRule<?> activityTestRule) throws IOException
    {
        log(String.format("Feature: %s", feature.getName()));

        if (!TextUtils.isEmpty(feature.getDescription()))
        {
            logDescription("\t", feature.getDescription());
        }

        List<ScenarioDefinition> backgrounds = filterScenariosBy(feature, "Background");
        List<ScenarioDefinition> scenarios = filterScenariosBy(feature, "Scenario");

        for (int i = 0; i < scenarios.size(); i++)
        {
            ScenarioDefinition scenario = scenarios.get(i);

            for (ScenarioDefinition background : backgrounds)
            {
                processScenario(background, stepDefinitions);
            }

            processScenario(scenario, stepDefinitions);

            if (i < (scenarios.size() - 1))
            {
                activityTestRule.restartActivity();
            }
        }
    }

    private List<ScenarioDefinition> filterScenariosBy(Feature feature, String keyword)
    {
        List<ScenarioDefinition> backgrounds = new ArrayList<>();

        for (ScenarioDefinition scenario : feature.getChildren())
        {
            if (TextUtils.equals(scenario.getKeyword(), keyword))
            {
                backgrounds.add(scenario);
            }
        }

        return backgrounds;
    }

    private void processScenario(ScenarioDefinition scenario, List<StepDefinition> stepDefinitions) throws IOException
    {
        log(String.format("\tScenario: %s", scenario.getName()));

        if (!TextUtils.isEmpty(scenario.getDescription()))
        {
            logDescription("\t\t", scenario.getDescription());
        }

        for (Step step : scenario.getSteps())
        {
            processStep(step, stepDefinitions);
        }
    }

    private void processStep(Step step, List<StepDefinition> stepDefinitions) throws IOException
    {
        String keyword = step.getKeyword().trim();
        String text = step.getText().trim();

        log(String.format("\t\t%s %s", keyword, text));

        for (StepDefinition stepDefinition : stepDefinitions)
        {
            if (stepDefinition.matches(text))
            {
                stepDefinition.invoke(text);
                return;
            }
        }

        throw new RuntimeException(String.format("Step definition not found for: '%s: %s'", keyword, text));
    }

    protected String fromAssets(String featurePath) throws IOException
    {
        InputStream stream = getClass().getClassLoader().getResourceAsStream(featurePath);

        return fromInputStream(stream);
    }

    protected String fromInputStream(InputStream featureInput) throws IOException
    {
        BufferedReader reader = new BufferedReader(new InputStreamReader(featureInput));
        StringBuilder builder = new StringBuilder();
        String line;

        while ((line = reader.readLine()) != null)
        {
            builder.append(line).append("\n");
        }

        reader.close();

        return builder.toString();
    }

    private void logDescription(String tab, String description)
    {
        for (String line : description.split("\n"))
        {
            log(String.format("%s%s", tab, line.trim()));
        }
    }

    private void log(String message)
    {
        System.out.println(message);
        System.out.flush();
    }

    protected void clickWithId(@IdRes int resourceId)
    {
        onView(withId(resourceId)).perform(click());
    }

    protected void clickWithText(@StringRes int resourceId)
    {
        onView(withText(resourceId)).perform(click());
    }

    protected void typeTextWithId(@IdRes int resourceId, String text)
    {
        onView(withId(resourceId)).perform(typeText(text));
    }

    protected void typeTextWithText(@StringRes int resourceId, String text)
    {
        onView(withText(resourceId)).perform(typeText(text));
    }

    protected void isVisibleWithId(@IdRes int resourceId)
    {
        onView(withId(resourceId)).check(matches(isDisplayed()));
    }

    protected void isVisibleWithText(@StringRes int resourceId)
    {
        onView(withText(resourceId)).check(matches(isDisplayed()));
    }
}