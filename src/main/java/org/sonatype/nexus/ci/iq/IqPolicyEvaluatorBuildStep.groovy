/*
 * Copyright (c) 2016-present Sonatype, Inc. All rights reserved.
 *
 * This program is licensed to you under the Apache License Version 2.0,
 * and you may not use this file except in compliance with the Apache License Version 2.0.
 * You may obtain a copy of the Apache License Version 2.0 at http://www.apache.org/licenses/LICENSE-2.0.
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the Apache License Version 2.0 is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the Apache License Version 2.0 for the specific language governing permissions and limitations there under.
 */
package org.sonatype.nexus.ci.iq

import org.sonatype.nexus.ci.config.NxiqConfiguration
import org.sonatype.nexus.ci.util.FormUtil
import org.sonatype.nexus.ci.util.IqUtil

import com.cloudbees.plugins.credentials.common.StandardListBoxModel
import hudson.Extension
import hudson.Launcher
import hudson.model.AbstractBuild
import hudson.model.AbstractProject
import hudson.model.BuildListener
import hudson.model.Job
import hudson.tasks.BuildStep
import hudson.tasks.BuildStepDescriptor
import hudson.tasks.Builder
import hudson.util.FormValidation
import hudson.util.ListBoxModel
import org.kohsuke.stapler.AncestorInPath
import org.kohsuke.stapler.DataBoundConstructor
import org.kohsuke.stapler.DataBoundSetter
import org.kohsuke.stapler.QueryParameter

class IqPolicyEvaluatorBuildStep
    extends Builder
    implements IqPolicyEvaluator, BuildStep
{
  public static final String NO_IQ_SERVER_ERR = "No Nexus IQ server with IQ Server ID: "

  String iqServerId

  String iqStage

  IqApplication iqApplication

  List<ScanPattern> iqScanPatterns

  List<ModuleExclude> iqModuleExcludes

  Boolean failBuildOnNetworkError

  String jobCredentialsId

  String advancedProperties

  @DataBoundConstructor
  @SuppressWarnings('ParameterCount')
  IqPolicyEvaluatorBuildStep(final String iqStage,
                             final IqApplication iqApplication,
                             final List<ScanPattern> iqScanPatterns,
                             final List<ModuleExclude> iqModuleExcludes,
                             final Boolean failBuildOnNetworkError,
                             final String jobCredentialsId,
                             final String advancedProperties)
  {
    this.jobCredentialsId = jobCredentialsId
    this.failBuildOnNetworkError = failBuildOnNetworkError
    this.iqScanPatterns = iqScanPatterns
    this.iqModuleExcludes = iqModuleExcludes
    this.iqStage = iqStage
    this.iqApplication = iqApplication
    this.advancedProperties = advancedProperties
  }

  @DataBoundSetter
  void setIqServerId(String iqServerId) {
    this.iqServerId = iqServerId
    NxiqConfiguration nxiqConfiguration = IqUtil.getNxiqConfiguration(this.iqServerId)
    if (!nxiqConfiguration) {
      throw new IllegalArgumentException(NO_IQ_SERVER_ERR + iqServerId)
    }
  }

  @Override
  boolean perform(AbstractBuild run, Launcher launcher, BuildListener listener)
      throws InterruptedException, IOException
  {
    IqPolicyEvaluatorUtil.
        evaluatePolicy(this, run, run.getWorkspace(), launcher, listener, run.getEnvironment(listener))
    return true
  }

  @Extension
  static final class DescriptorImpl
      extends BuildStepDescriptor<Builder>
      implements IqPolicyEvaluatorDescriptor
  {

    @Override
    String getDisplayName() {
      Messages.IqPolicyEvaluation_DisplayName()
    }

    @Override
    boolean isApplicable(final Class<? extends AbstractProject> jobType) {
      return true
    }

    @Override
    ListBoxModel doFillIqServerIdItems() {
      IqUtil.doFillIqServerIdItems()
    }

    @Override
    FormValidation doCheckIqStage(@QueryParameter String value) {
      FormValidation.validateRequired(value)
    }

    @Override
    ListBoxModel doFillIqStageItems(@QueryParameter String jobCredentialsId, @AncestorInPath Job job,
                                    @QueryParameter String iqServerId)
    {
      // JobCredentialsId is an empty String if not set
      IqUtil.doFillIqStageItems(jobCredentialsId, job, iqServerId)
    }

    @Override
    FormValidation doCheckAdvancedProperties(@QueryParameter String advancedProperties) {
      FormValidation.ok()
    }

    @Override
    FormValidation doCheckScanPattern(@QueryParameter String value) {
      FormValidation.ok()
    }

    @Override
    FormValidation doCheckModuleExclude(@QueryParameter final String value) {
      FormValidation.ok()
    }

    @Override
    FormValidation doCheckFailBuildOnNetworkError(@QueryParameter String value) {
      FormValidation.validateRequired(value)
    }

    @Override
    ListBoxModel doFillJobCredentialsIdItems(@AncestorInPath Job job, @QueryParameter String iqServerId) {
      NxiqConfiguration nxiqConfiguration = IqUtil.getNxiqConfiguration(iqServerId)
      if (nxiqConfiguration) {
        FormUtil.newCredentialsItemsListBoxModel(nxiqConfiguration.serverUrl, nxiqConfiguration.credentialsId,
            job)
      }
      else {
        new StandardListBoxModel().includeEmptyValue()
      }
    }

    @Override
    FormValidation doVerifyCredentials(@QueryParameter String jobCredentialsId, @AncestorInPath Job job)
    {
      IqUtil.verifyJobCredentials(jobCredentialsId, job)
    }
  }
}
