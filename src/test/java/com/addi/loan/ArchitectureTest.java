package com.addi.loan;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.*;

/**
 * Architecture Tests - Enforce ADR145 patterns
 *
 * These tests enforce the "Thin Triggers, Smart Workflows, Dumb Activities" pattern.
 * They will fail if developers violate the architectural constraints.
 */
@AnalyzeClasses(
    packages = "com.addi.loan",
    importOptions = ImportOption.DoNotIncludeTests.class
)
public class ArchitectureTest {

    @ArchTest
    static final ArchRule workflows_must_not_have_cdi_injection =
        noClasses()
            .that().haveSimpleNameEndingWith("WorkflowImpl")
            .should().beAnnotatedWith(ApplicationScoped.class)
            .because("Workflows must be deterministic with zero I/O - no CDI injection allowed (ADR145)");

    @ArchTest
    static final ArchRule workflows_must_not_use_inject =
        noFields()
            .that().areDeclaredInClassesThat().haveSimpleNameEndingWith("WorkflowImpl")
            .should().beAnnotatedWith(Inject.class)
            .because("Workflows must be deterministic - use Workflow.newActivityStub() instead (ADR145)");

    @ArchTest
    static final ArchRule activities_should_be_application_scoped =
        classes()
            .that().haveSimpleNameEndingWith("ActivitiesImpl")
            .should().beAnnotatedWith(ApplicationScoped.class)
            .because("Activity implementations must be CDI beans (ADR145)");

    @ArchTest
    static final ArchRule resources_should_be_thin_triggers =
        noClasses()
            .that().haveSimpleNameEndingWith("Resource")
            .should().dependOnClassesThat().haveSimpleNameEndingWith("Repository")
            .because("REST endpoints are thin triggers - they should only call WorkflowClient (ADR145)");

    @ArchTest
    static final ArchRule features_should_be_isolated =
        noClasses()
            .that().resideInAPackage("..features.submitloan..")
            .should().dependOnClassesThat().resideInAPackage("..features.loanevent..")
            .because("VSA slices must be isolated from each other (ADR145)");

    @ArchTest
    static final ArchRule infrastructure_should_not_depend_on_features =
        noClasses()
            .that().resideInAPackage("..infrastructure..")
            .should().dependOnClassesThat().resideInAPackage("..features..")
            .because("Infrastructure layer should not depend on feature slices (ADR145)");

    @ArchTest
    static final ArchRule workflow_interfaces_should_be_in_feature_package =
        classes()
            .that().haveSimpleNameEndingWith("Workflow")
            .and().areInterfaces()
            .should().resideInAPackage("..features..")
            .because("Workflow interfaces are part of feature slices (ADR145)");

    @ArchTest
    static final ArchRule activity_interfaces_should_be_in_feature_package =
        classes()
            .that().haveSimpleNameEndingWith("Activities")
            .and().areInterfaces()
            .should().resideInAPackage("..features..")
            .because("Activity interfaces are part of feature slices (ADR145)");
}
