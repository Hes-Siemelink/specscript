# Instacli → SpecScript Library Architecture Refactoring Plan

**Date**: September 6, 2025  
**Status**: Planning Phase  
**Objective**: Transform SpecScript into a library and make Instacli depend on it while maintaining separate repositories

**Important Note**: All Instacli refactoring work (Phases 2-4) will be performed on a dedicated branch to avoid disrupting the main Instacli repository during development. The main branch will remain stable until the refactoring is complete and tested.

##  Current State Analysis

**SpecScript Repo (this)**:
- Full codebase extracted from Instacli
- Renamed from "Instacli" to "SpecScript" branding
- Complete implementation with all commands and tests
- 551 lines in README (identical size to original)

**Instacli Repo (../instacli)**:  
- Original repository with "Instacli" branding
- Has `instacli-spec/` directory (not renamed)
- Complete implementation (still independent)
- Build artifacts and original README intact

##  Target Architecture

```
┌─────────────────┐    ┌──────────────────┐    ┌─────────────────┐
│   instacli/     │    │   specscript/    │    │  dev-workspace/ │
│   (skeleton)    │    │   (library)      │    │  (multi-repo)   │
│                 │    │                  │    │                 │
│ • Original README│◄──►│ • Core engine    │◄──►│ • Both as       │
│ • Gradle wrapper │    │ • All commands   │    │   submodules    │
│ • Thin CLI shell│    │ • Language impl  │    │ • Unified build │
│ • Instacli brand │    │ • Tests & specs  │    │ • Cross-repo    │
│ • Depends on     │    │ • SpecScript     │    │   development   │
│   specscript lib │    │   branding       │    │ • Easy refactor │
└─────────────────┘    └──────────────────┘    └─────────────────┘
```

##  Implementation Plan

### **Phase 1: Prepare SpecScript as Library** 
**Effort**: Low | **Risk**: Low | **Duration**: 1 day

**Goals**:
- Make SpecScript publishable as a library
- Enable Instacli to consume it as a dependency
- Maintain CLI functionality for standalone use

**Tasks**:
1. **Configure Gradle for library publishing**
   - Add Maven publishing plugin
   - Configure artifact generation (JAR, sources, docs)
   - Set up local Maven repository publishing
   - Create library-specific build configuration

**Deliverables**:
- SpecScript library JAR publishable to Maven
- Working CLI that uses the same internal code
- Basic library consumption capability

### **Phase 2: Transform Instacli to Consumer**
**Effort**: High | **Risk**: Medium | **Duration**: 2-3 days

**Goals**:
- Gut Instacli implementation and make it consume SpecScript
- Preserve user experience and branding
- Maintain backward compatibility

**Tasks**:
1. **Remove Instacli implementation**
   - Delete `src/main/kotlin/instacli/` directory (except CLI bootstrap)
   - From the `instacli-spec/` directory, remove `commands/`, `language/` and `scratchpad/` but retain `cli/` and `README.md`
   - Clean up build configuration
   - Preserve gradle wrapper, README, and project structure

2. **Create minimal CLI bootstrap**
   - Minimal `Main.kt` that delegates to SpecScript library
   - Pass-through all CLI arguments and options
   - Preserve exact same CLI interface and behavior
   - Handle version reporting and help text properly

3. **Update build configuration**
   - Add dependency on SpecScript library
   - Remove unnecessary dependencies (now in library)
   - Configure fat JAR to include SpecScript
   - Ensure build outputs work identically to before

4. **Preserve Instacli identity**
   - Keep original README with "Instacli" branding
   - Maintain version numbering scheme
   - Keep samples and documentation structure
   - Ensure error messages still show "Instacli"

**Deliverables**:
- Instacli as thin wrapper around SpecScript
- Identical user experience to original
- Significantly reduced codebase in Instacli

### **Phase 3: Create Multi-Repo Development Setup**
**Effort**: Medium | **Risk**: Low | **Duration**: 1 day

**Goals**:
- Enable efficient cross-repo development
- Simplify testing and refactoring across both repos
- Coordinate releases and dependencies

**Tasks**:
1. **Create dev-workspace repository**
   ```
   dev-workspace/
   ├── .gitmodules
   ├── instacli/          (git submodule → ../instacli)
   ├── specscript/        (git submodule → ../specscript)  
   ├── build.gradle.kts   (composite build)
   ├── settings.gradle.kts
   ├── scripts/
   │   ├── setup.sh       (development environment setup)
   │   ├── test-all.sh    (run tests across both repos)
   │   └── sync-deps.sh   (update cross-repo dependencies)
   └── README.md
   ```

2. **Configure Gradle composite build**
   - Enable `includeBuild()` for both repositories
   - Allow SpecScript changes to be immediately visible in Instacli
   - Unified dependency resolution
   - Cross-repo testing capabilities

3. **Development workflow automation**
   - Setup scripts for new developers
   - Cross-repo refactoring helpers  
   - Automated dependency version updates
   - Integration test suites

**Deliverables**:
- Functional multi-repo development environment
- Automated setup and testing scripts
- Documentation for development workflow

### **Phase 4: Gradual Code Migration** 
**Effort**: Variable | **Risk**: Low | **Duration**: Ongoing

**Goals**:
- Move Instacli-specific code back from SpecScript to Instacli
- Keep core engine generic in SpecScript
- Maintain clean separation of concerns

**Tasks**:
1. **Design clean API boundaries**
   - Define core interfaces: `ScriptEngine`, `CommandRegistry`, etc.
   - Separate internal implementation from public API
   - Create facade classes for easy consumption
   - Version the API for stability

2. **Identify migration candidates**
   - Instacli-specific commands and features
   - Branding and messaging
   - Default configurations
   - CLI-specific behaviors

3. **Design extension mechanisms**
   - Plugin system for custom commands
   - Configuration override mechanisms
   - Custom branding injection points
   - Extensible CLI framework

4. **Gradual migration**
   - Move features one by one
   - Test each migration thoroughly
   - Maintain API compatibility
   - Update documentation

**Deliverables**:
- Clean separation between generic engine and specific CLI
- Extensible architecture for future customizations
- Well-defined migration patterns for other features

##  Technical Considerations

### **Dependency Management**
- **SpecScript Versioning**: Use semantic versioning (MAJOR.MINOR.PATCH)
- **API Stability**: Design for backward compatibility from v1.0
- **Instacli Dependencies**: Pin to specific SpecScript versions, upgrade deliberately
- **Transitive Dependencies**: Manage carefully to avoid conflicts

### **Build & Release Strategy**  
- **Independent CI/CD**: Each repo has separate build pipeline
- **Library Publishing**: SpecScript publishes to Maven Central (or private registry)
- **Integration Testing**: Dev-workspace runs cross-repo integration tests
- **Release Coordination**: Document release process for both repos

### **API Design Principles**
- **Framework Agnostic**: Core engine shouldn't assume CLI usage
- **Extensible**: Allow custom commands and behaviors
- **Stable**: Minimize breaking changes through careful design
- **Documented**: Comprehensive API documentation and examples

##  Risks & Mitigations

| Risk | Impact | Probability | Mitigation Strategy |
|------|--------|-------------|-------------------|
| **API Instability** | High | Medium | • Start with broader API, narrow over time<br>• Use semantic versioning rigorously<br>• Extensive integration testing |
| **Development Complexity** | Medium | High | • Multi-repo tooling and automation<br>• Clear development workflow documentation<br>• IDE setup guides |
| **User Experience Regression** | High | Low | • Maintain identical CLI interface<br>• Comprehensive compatibility testing<br>• User acceptance testing |
| **Build/Dependency Issues** | Medium | Medium | • Gradle composite builds<br>• Dependency convergence testing<br>• Clear versioning strategy |
| **Code Duplication** | Low | Medium | • Well-defined API boundaries<br>• Regular refactoring reviews<br>• Shared utilities in library |

##  Success Criteria

### **Phase 1 Complete** ✅
- [x] SpecScript library published to local Maven repository
- [x] CLI functionality unchanged
- [x] All existing tests passing (392 tests, 100% success rate)

### **Phase 2 Complete** ✅
- [x] All changes are on a dedicated branch `specscript`
- [x] Instacli codebase reduced by >95% (6 files vs 117 in SpecScript)
- [x] Identical user experience to original Instacli
- [x] Specification tests still run -- testing the README and remaining `instacli-spec/cli/` documentation
- [x] All tests passing in both repos
- [x] Fat JAR builds successfully

### **Phase 3**
- [ ] Dev-workspace repository functional
- [ ] Cross-repo changes work seamlessly
- [ ] Automated testing across both repos
- [ ] Developer onboarding documentation

### **Phase 4**
- [ ] Clear migration patterns established
- [ ] At least one feature migrated back to Instacli
- [ ] Extension mechanisms proven to work
- [ ] Architecture documentation updated

##  **Recommended Next Steps**

**Immediate (Phase 1)**:
1. Configure SpecScript Gradle for library publishing
2. Define public API interfaces and facades  
3. Test library consumption with simple test project
4. Document API contracts and usage examples

**Short-term (Phase 2)**:
1. Create Instacli skeleton with SpecScript dependency
2. Implement CLI bootstrap and pass-through
3. Test complete user workflows
4. Validate build and packaging

**Medium-term (Phase 3)**:
1. Set up dev-workspace repository
2. Configure composite builds
3. Create development automation scripts
4. Test cross-repo development workflow

This plan balances the need for major architectural changes with risk mitigation and maintaining a good developer experience throughout the refactoring process.