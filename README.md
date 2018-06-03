# Talk about and Demo Eclipse IDE Parallel builds feature

This repo is an Eclipse IDE plugin to load into a vanilla Eclipse Platform (ideally without anything like JDT, CDT, PDE, m2e installed... just the very minumal stuff).

You can simply create a few projects and add the "WaitBuilder" to those projects. Then, whenever you'll build the workspace explicitly (auto-build is ignored), you'll see 2 png created and opened inside the IDE:
* One is the dependency graph for the project, which also highlight conflicting scheduling rules between WaitBuilder on different project. The scheduling rule check only works for WaitBuilder, other builders are ignored.
* The other is a Gantt chart that shows how the sequence of build has happened. The content of this GanttChart only take into account the execution of the WaitBuilder, other builders are not reported (they could be using existing API, but https://bugs.eclipse.org/bugs/show_bug.cgi?id=535476 currently prevent good usage).

Each project can have a `waitBuilder.properties` file, looking like
```
duration=1000
schedulingRule=this
```
where
* `duration` is the duration for which the project will build (ie related thread sleep) in millisecond
* `schedulingRule` is the scheduling rule to apply to the builder when building the project. Possible values are `this` for the current project, `/` for the workspace root (will create conflicts and disable parallel builds) or the name of another project in the workspace (may create conflict).

Playing with `duration` allows to show how the shortest build become available earlier with the number of builders. Playing with scheduling rules can highlight how conflicting scheduling rule to slow down the build and how optimizing scheduling rules for builders is critical.