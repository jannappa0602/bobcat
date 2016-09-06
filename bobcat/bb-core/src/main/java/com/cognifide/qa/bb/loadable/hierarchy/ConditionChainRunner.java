/*
 * Copyright 2016 Cognifide Ltd..
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.cognifide.qa.bb.loadable.hierarchy;

import com.cognifide.qa.bb.loadable.tracker.ConditionStatus;
import com.cognifide.qa.bb.loadable.tracker.ConditionProgressTracker;
import com.cognifide.qa.bb.loadable.context.LoadableComponentContext;
import com.cognifide.qa.bb.loadable.condition.LoadableComponentCondition;
import com.cognifide.qa.bb.loadable.exception.LoadableConditionException;
import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Singleton;

import java.util.Stack;

@Singleton
public class ConditionChainRunner {

  @Inject
  private Injector injector;

  public void chainCheck(ConditionStack conditionStack) throws LoadableConditionException {
    Stack<LoadableComponentContext> stack = conditionStack.getLoadableContextStack();
    ConditionProgressTracker progressTracker = new ConditionProgressTracker(stack);

    while (!stack.isEmpty()) {
      LoadableComponentContext loadableContext = stack.pop();
      if (loadableContext.getConditionData() != null) {
        progressTracker.stepStart(loadableContext);
        LoadableComponentCondition componentCondition = produceInitializedCondition(loadableContext);
        evaluateCondition(componentCondition, loadableContext, progressTracker);
      }
    }
  }

  private LoadableComponentCondition produceInitializedCondition(LoadableComponentContext loadableContext) {
    return (LoadableComponentCondition) injector.getInstance(loadableContext.getConditionData().getCondClass());
  }

  private void evaluateCondition(LoadableComponentCondition componentCondition,
          LoadableComponentContext loadableContext, ConditionProgressTracker progressTracker) throws
          LoadableConditionException {
    boolean result = false;
    Exception exception = null;
    try {
      result = componentCondition.check(loadableContext.getElement(), loadableContext.getConditionData());
    } catch (Exception ex) {
      exception = ex;
    } finally {
      manageEvaluationResult(result, exception, progressTracker);
    }
  }

  private void manageEvaluationResult(boolean result, Exception exception,
          ConditionProgressTracker progressTracker) throws LoadableConditionException {
    if (result == false || exception != null) {
      progressTracker.provideStepResult(ConditionStatus.FAIL);
      throw new LoadableConditionException(progressTracker.produceConditionTraceInfo(exception));
    } else {
      progressTracker.provideStepResult(ConditionStatus.SUCCESS);
    }
  }

}