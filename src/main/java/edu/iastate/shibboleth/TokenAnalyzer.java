package edu.iastate.shibboleth;

/*-
 * #%L
 * shibboleth-maven-plugin
 * %%
 * Copyright (C) 2021 - 2022 Iowa State University
 * %%
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
 * #L%
 */

import com.github.javaparser.JavaParser;
import com.github.javaparser.ParseResult;
import com.github.javaparser.Problem;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.body.BodyDeclaration;
import com.github.javaparser.ast.body.CallableDeclaration;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.ConstructorDeclaration;
import com.github.javaparser.ast.body.EnumDeclaration;
import com.github.javaparser.ast.body.InitializerDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.TypeDeclaration;
import com.github.javaparser.ast.body.VariableDeclarator;
import com.github.javaparser.ast.expr.ArrayAccessExpr;
import com.github.javaparser.ast.expr.ArrayCreationExpr;
import com.github.javaparser.ast.expr.ArrayInitializerExpr;
import com.github.javaparser.ast.expr.AssignExpr;
import com.github.javaparser.ast.expr.BinaryExpr;
import com.github.javaparser.ast.expr.BooleanLiteralExpr;
import com.github.javaparser.ast.expr.CastExpr;
import com.github.javaparser.ast.expr.ClassExpr;
import com.github.javaparser.ast.expr.ConditionalExpr;
import com.github.javaparser.ast.expr.EnclosedExpr;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.FieldAccessExpr;
import com.github.javaparser.ast.expr.InstanceOfExpr;
import com.github.javaparser.ast.expr.LambdaExpr;
import com.github.javaparser.ast.expr.LiteralStringValueExpr;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.MethodReferenceExpr;
import com.github.javaparser.ast.expr.NameExpr;
import com.github.javaparser.ast.expr.NullLiteralExpr;
import com.github.javaparser.ast.expr.ObjectCreationExpr;
import com.github.javaparser.ast.expr.SimpleName;
import com.github.javaparser.ast.expr.SuperExpr;
import com.github.javaparser.ast.expr.SwitchExpr;
import com.github.javaparser.ast.expr.ThisExpr;
import com.github.javaparser.ast.expr.TypeExpr;
import com.github.javaparser.ast.expr.UnaryExpr;
import com.github.javaparser.ast.expr.VariableDeclarationExpr;
import com.github.javaparser.ast.stmt.AssertStmt;
import com.github.javaparser.ast.stmt.BlockStmt;
import com.github.javaparser.ast.stmt.BreakStmt;
import com.github.javaparser.ast.stmt.CatchClause;
import com.github.javaparser.ast.stmt.ContinueStmt;
import com.github.javaparser.ast.stmt.DoStmt;
import com.github.javaparser.ast.stmt.ExplicitConstructorInvocationStmt;
import com.github.javaparser.ast.stmt.ExpressionStmt;
import com.github.javaparser.ast.stmt.ForEachStmt;
import com.github.javaparser.ast.stmt.ForStmt;
import com.github.javaparser.ast.stmt.IfStmt;
import com.github.javaparser.ast.stmt.LabeledStmt;
import com.github.javaparser.ast.stmt.ReturnStmt;
import com.github.javaparser.ast.stmt.Statement;
import com.github.javaparser.ast.stmt.SwitchEntry;
import com.github.javaparser.ast.stmt.SwitchStmt;
import com.github.javaparser.ast.stmt.SynchronizedStmt;
import com.github.javaparser.ast.stmt.ThrowStmt;
import com.github.javaparser.ast.stmt.TryStmt;
import com.github.javaparser.ast.stmt.WhileStmt;
import com.github.javaparser.ast.type.Type;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

/**
 * @author Ali Ghanbari (alig@iastate.edu)
 */
public class TokenAnalyzer {
    private static final JavaParser JAVA_PARSER;

    static {
        JAVA_PARSER = new JavaParser();
    }

    private Map<String, Integer> tokensVectorOriginal;

    private Map<String, Integer> tokensVectorPatched;

    private Set<String> tokensUnion;

    public Map<String, Integer> getTokensVectorOriginal() {
        return this.tokensVectorOriginal;
    }

    public Map<String, Integer> getTokensVectorPatched() {
        return this.tokensVectorPatched;
    }

    public Set<String> getTokensUnion() {
        return this.tokensUnion;
    }

    public double calculateSimilarity(final Collection<File> originalFiles,
                                      final Collection<File> patchedFiles,
                                      final Collection<String> targetMethods,
                                      final VectorAnalyzer calculator) {
        if (originalFiles == null || originalFiles.isEmpty()) {
            throw new IllegalArgumentException();
        }
        if (patchedFiles == null || patchedFiles.isEmpty()) {
            throw new IllegalArgumentException();
        }
        if (targetMethods == null || targetMethods.isEmpty()) {
            throw new IllegalArgumentException();
        }
        if (calculator == null) {
            throw new IllegalArgumentException();
        }
        this.tokensVectorOriginal = new HashMap<>();
        this.tokensVectorPatched = new HashMap<>();
        try {
            for (final File file : originalFiles) {
                final ParseResult<CompilationUnit> res = JAVA_PARSER.parse(file);
                res.ifSuccessful(cu -> processCompilationUnit(cu, this.tokensVectorOriginal, targetMethods));
                if (!res.isSuccessful()) {
                    printProblems(file, res.getProblems());
                }
            }
            for (final File file : patchedFiles) {
                final ParseResult<CompilationUnit> res = JAVA_PARSER.parse(file);
                res.ifSuccessful(cu -> processCompilationUnit(cu, this.tokensVectorPatched, targetMethods));
                if (!res.isSuccessful()) {
                    printProblems(file, res.getProblems());
                }
            }
        } catch (final FileNotFoundException exception) {
            throw new RuntimeException(exception.getMessage(), exception.getCause());
        }
        this.tokensUnion = new TreeSet<>();
        this.tokensUnion.addAll(tokensVectorOriginal.keySet());
        this.tokensUnion.addAll(tokensVectorPatched.keySet());
        final List<Integer> expandedTokensVectorOriginal = new ArrayList<>(this.tokensUnion.size());
        final List<Integer> expandedTokensVectorPatched = new ArrayList<>(this.tokensUnion.size());
        for (final String token : this.tokensUnion) {
            expandedTokensVectorOriginal.add(this.tokensVectorOriginal.getOrDefault(token, 0));
            expandedTokensVectorPatched.add(this.tokensVectorPatched.getOrDefault(token, 0));
        }
        return calculator.calculateSimDist(expandedTokensVectorOriginal, expandedTokensVectorPatched);
    }

    private void printProblems(final File problematicFile, final List<Problem> problems) {
        for (final Problem problem : problems) {
            System.out.printf("Problem (%s):%n", problematicFile.getAbsolutePath());
            System.out.println(problem.getMessage());
            problem.getCause().ifPresent(Throwable::printStackTrace);
            System.out.println("--------");
        }
    }

    private void processCompilationUnit(final CompilationUnit compilationUnit,
                                        final Map<String, Integer> tokensVector,
                                        final Collection<String> targetMethods) {
        for (final TypeDeclaration<?> type : compilationUnit.getTypes()) {
            if (type.isClassOrInterfaceDeclaration()) {
                processClassOrInterfaceDeclaration(type.asClassOrInterfaceDeclaration(),
                        tokensVector, targetMethods);
            } else if (type.isEnumDeclaration()) {
                processEnumDeclaration(type.asEnumDeclaration(), tokensVector, targetMethods);
            }
        }
    }

    private void processClassInitializer(final InitializerDeclaration initializerDeclaration,
                                        final Map<String, Integer> tokensVector,
                                        final Collection<String> targetMethods) {
        for (final String methodName : targetMethods) {
            if (methodName.contains("<clinit>")) {
                initializerDeclaration.getBody().ifBlockStmt(blockStmt -> processBlockStatement(blockStmt, tokensVector));
                return;
            }
        }
    }

    private void processEnumDeclaration(final EnumDeclaration enumDeclaration,
                                        final Map<String, Integer> tokensVector,
                                        final Collection<String> targetMethods) {
        for (final BodyDeclaration<?> member : enumDeclaration.getMembers()) {
            if (member.isMethodDeclaration()) {
                processMethodDeclaration(member.asMethodDeclaration(), tokensVector, targetMethods);
            } else if (member.isConstructorDeclaration()) {
                processConstructorDeclaration(member.asConstructorDeclaration(), tokensVector, targetMethods);
            }
        }
    }

    private void processClassOrInterfaceDeclaration(final ClassOrInterfaceDeclaration classOrInterfaceDeclaration,
                                                    final Map<String, Integer> tokensVector,
                                                    final Collection<String> targetMethods) {
        for (final BodyDeclaration<?> member : classOrInterfaceDeclaration.getMembers()) {
            if (member.isClassOrInterfaceDeclaration()) {
                processClassOrInterfaceDeclaration(member.asClassOrInterfaceDeclaration(),
                        tokensVector, targetMethods);
            } else if (member.isEnumDeclaration()) {
                processEnumDeclaration(member.asEnumDeclaration(), tokensVector, targetMethods);
            } else if (member.isInitializerDeclaration()) {
                processClassInitializer(member.asInitializerDeclaration(), tokensVector, targetMethods);
            } else if (member.isMethodDeclaration()) {
                processMethodDeclaration(member.asMethodDeclaration(), tokensVector, targetMethods);
            } else if (member.isConstructorDeclaration()) {
                processConstructorDeclaration(member.asConstructorDeclaration(), tokensVector, targetMethods);
            }
        }
    }

    private void processMethodDeclaration(final MethodDeclaration methodDeclaration,
                                          final Map<String, Integer> tokensVector,
                                          final Collection<String> targetMethods) {
        if (isTargetMethod(buildMethodFullSignature(methodDeclaration), targetMethods)) {
            methodDeclaration.getBody().ifPresent(blockStmt -> processBlockStatement(blockStmt, tokensVector));
        }
    }

    private void processConstructorDeclaration(final ConstructorDeclaration constructorDeclaration,
                                          final Map<String, Integer> tokensVector,
                                          final Collection<String> targetMethods) {
        if (isTargetMethod(buildMethodFullSignature(constructorDeclaration), targetMethods)) {
            constructorDeclaration.getBody().ifBlockStmt(blockStmt -> processBlockStatement(blockStmt, tokensVector));
        }
    }

    private boolean isTargetMethod(final String sig, final Collection<String> targetMethods) {
        for (final String target : targetMethods) {
            if (matches(target, sig)) {
                return true;
            }
        }
        return false;
    }

    private void processStatement(final Statement statement, final Map<String, Integer> tokensVector) {
        if (statement instanceof BlockStmt) {
            processBlockStatement((BlockStmt) statement, tokensVector);
        } else if (statement instanceof IfStmt) {
            tokensVector.put("if", 1 + tokensVector.getOrDefault("if", 0));
            processStatement(((IfStmt) statement).getThenStmt(), tokensVector);
            final Statement elseStmt = ((IfStmt) statement).getElseStmt().orElse(null);
            if (elseStmt != null) {
                tokensVector.put("else", 1 + tokensVector.getOrDefault("else", 0));
                processStatement(elseStmt, tokensVector);
            }
            processExpression(((IfStmt) statement).getCondition(), tokensVector);
        } else if (statement instanceof WhileStmt) {
            tokensVector.put("while", 1 + tokensVector.getOrDefault("while", 0));
            processStatement(((WhileStmt) statement).getBody(), tokensVector);
            processExpression(((WhileStmt) statement).getCondition(), tokensVector);
        } else if (statement instanceof DoStmt) {
            tokensVector.put("do", 1 + tokensVector.getOrDefault("do", 0));
            tokensVector.put("while", 1 + tokensVector.getOrDefault("while", 0));
            processStatement(((DoStmt) statement).getBody(), tokensVector);
            processExpression(((DoStmt) statement).getCondition(), tokensVector);
        } else if (statement instanceof ForEachStmt) {
            tokensVector.put("for:", 1 + tokensVector.getOrDefault("for:", 0));
            processStatement(((ForEachStmt) statement).getBody(), tokensVector);
            final VariableDeclarator variableDeclarator = ((ForEachStmt) statement).getVariableDeclarator();
            final String typeName = variableDeclarator.getType().asString();
            tokensVector.put(typeName, 1 + tokensVector.getOrDefault(typeName, 0));
            final String varName = variableDeclarator.getName().asString();
            tokensVector.put(varName, 1 + tokensVector.getOrDefault(varName, 0));
            processExpression(((ForEachStmt) statement).getIterable(), tokensVector);
        } else if (statement instanceof ContinueStmt) {
            ((ContinueStmt) statement).getLabel()
                    .map(SimpleName::asString)
                    .ifPresent(labelName -> tokensVector.put(labelName, 1 + tokensVector.getOrDefault(labelName, 0)));
            tokensVector.put("continue", 1 + tokensVector.getOrDefault("continue", 0));
        } else if (statement instanceof ExpressionStmt) {
            processExpression(((ExpressionStmt) statement).getExpression(), tokensVector);
        } else if (statement instanceof LabeledStmt) {
            final String label = ((LabeledStmt) statement).getLabel().asString();
            tokensVector.put(label, 1 + tokensVector.getOrDefault(label, 0));
            processStatement(((LabeledStmt) statement).getStatement(), tokensVector);
        } else if (statement instanceof ReturnStmt) {
            tokensVector.put("return", 1 + tokensVector.getOrDefault("return", 0));
            ((ReturnStmt) statement).getExpression().ifPresent(expression -> processExpression(expression, tokensVector));
        } else if (statement instanceof BreakStmt) {
            ((BreakStmt) statement).getLabel()
                    .map(SimpleName::asString)
                    .ifPresent(labelName -> tokensVector.put(labelName, 1 + tokensVector.getOrDefault(labelName, 0)));
            tokensVector.put("break", 1 + tokensVector.getOrDefault("break", 0));
        } else if (statement instanceof AssertStmt) {
            tokensVector.put("assert", 1 + tokensVector.getOrDefault("assert", 0));
            processExpression(((AssertStmt) statement).getCheck(), tokensVector);
            ((AssertStmt) statement).getMessage().ifPresent(expression -> processExpression(expression, tokensVector));
        } else if (statement instanceof ExplicitConstructorInvocationStmt) {
            if (((ExplicitConstructorInvocationStmt) statement).isThis()) {
                tokensVector.put("this", 1 + tokensVector.getOrDefault("this", 0));
            } else {
                tokensVector.put("super", 1 + tokensVector.getOrDefault("super", 0));
            }
            ((ExplicitConstructorInvocationStmt) statement).getArguments().forEach(expression -> processExpression(expression, tokensVector));
        } else if (statement instanceof ForStmt) {
            tokensVector.put("for", 1 + tokensVector.getOrDefault("for", 0));
            processStatement(((ForStmt) statement).getBody(), tokensVector);
            ((ForStmt) statement).getInitialization().forEach(expression -> processExpression(expression, tokensVector));
            ((ForStmt) statement).getCompare().ifPresent(expression -> processExpression(expression, tokensVector));
            ((ForStmt) statement).getUpdate().forEach(expression -> processExpression(expression, tokensVector));
        } else if (statement instanceof ThrowStmt) {
            tokensVector.put("throw", 1 + tokensVector.getOrDefault("throw", 0));
            processExpression(((ThrowStmt) statement).getExpression(), tokensVector);
        } else if (statement instanceof TryStmt) {
            tokensVector.put("try", 1 + tokensVector.getOrDefault("try", 0));
            processBlockStatement(((TryStmt) statement).getTryBlock(), tokensVector);
            ((TryStmt) statement).getCatchClauses().stream()
                    .map(CatchClause::getBody)
                    .forEach(catchBody -> {
                        tokensVector.put("catch", 1 + tokensVector.getOrDefault("catch", 0));
                        processBlockStatement(catchBody, tokensVector);
                    });
            ((TryStmt) statement).getFinallyBlock()
                    .ifPresent(finallyBody -> {
                        tokensVector.put("finally", 1 + tokensVector.getOrDefault("finally", 0));
                        processBlockStatement(finallyBody, tokensVector);
                    });
        } else if (statement instanceof SwitchStmt) {
            tokensVector.put("switch", 1 + tokensVector.getOrDefault("switch", 0));
            processExpression(((SwitchStmt) statement).getSelector(), tokensVector);
            ((SwitchStmt) statement).getEntries().forEach(entry -> processSwitchEntry(entry, tokensVector));
        } else if (statement instanceof SynchronizedStmt) {
            tokensVector.put("synchronized", 1 + tokensVector.getOrDefault("synchronized", 0));
            processExpression(((SynchronizedStmt) statement).getExpression(), tokensVector);
            processBlockStatement(((SynchronizedStmt) statement).getBody(), tokensVector);
        }
    }

    private void processSwitchEntry(final SwitchEntry entry, final Map<String, Integer> tokensVector) {
        entry.getStatements().forEach(statement -> {
            processStatement(statement, tokensVector);
        });
        entry.getLabels().forEach(expression -> processExpression(expression, tokensVector));
        if (entry.getLabels().isEmpty()) {
            tokensVector.put("default", 1 + tokensVector.getOrDefault("default", 0));
        } else {
            tokensVector.put("case", 1 + tokensVector.getOrDefault("case", 0));
        }
    }

    private void processBlockStatement(final BlockStmt blockStmt, final Map<String, Integer> tokensVector) {
        for (final Statement stmt : blockStmt.getStatements()) {
            processStatement(stmt, tokensVector);
        }
    }

    private void processExpression(final Expression expression, final Map<String, Integer> tokensVector) {
        if (expression instanceof FieldAccessExpr) {
            final String fieldName = ((FieldAccessExpr) expression).getName().asString();
            tokensVector.put(fieldName, 1 + tokensVector.getOrDefault(fieldName, 0));
            processExpression(((FieldAccessExpr) expression).getScope(), tokensVector);
        } else if (expression instanceof ArrayInitializerExpr) {
            ((ArrayInitializerExpr) expression).getValues().forEach(e -> processExpression(e, tokensVector));
        } else if (expression instanceof TypeExpr) {
            final String typeName = ((TypeExpr) expression).getType().asString();
            tokensVector.put(typeName, 1 + tokensVector.getOrDefault(typeName, 0));
        } else if (expression instanceof BinaryExpr) {
            final String operator = ((BinaryExpr) expression).getOperator().asString();
            tokensVector.put(operator, 1 + tokensVector.getOrDefault(operator, 0));
            processExpression(((BinaryExpr) expression).getLeft(), tokensVector);
            processExpression(((BinaryExpr) expression).getRight(), tokensVector);
        } else if (expression instanceof SuperExpr) {
            tokensVector.put("super", 1 + tokensVector.getOrDefault("super", 0));
        } else if (expression instanceof UnaryExpr) {
            final String operator = ((UnaryExpr) expression).getOperator().asString();
            tokensVector.put(operator, 1 + tokensVector.getOrDefault(operator, 0));
            processExpression(((UnaryExpr) expression).getExpression(), tokensVector);
        } else if (expression instanceof ObjectCreationExpr) {
            tokensVector.put("new", 1 + tokensVector.getOrDefault("new", 0));
            final String typeName = ((ObjectCreationExpr) expression).getType().getName().asString();
            tokensVector.put(typeName, 1 + tokensVector.getOrDefault(typeName, 0));
            ((ObjectCreationExpr) expression).getScope().ifPresent(e -> processExpression(e, tokensVector));
            ((ObjectCreationExpr) expression).getArguments().forEach(e -> processExpression(e, tokensVector));
        } else if (expression instanceof NullLiteralExpr) {
            tokensVector.put("null", 1 + tokensVector.getOrDefault("null", 0));
        } else if (expression instanceof BooleanLiteralExpr) {
            final String value = String.valueOf(((BooleanLiteralExpr) expression).getValue());
            tokensVector.put(value, 1 + tokensVector.getOrDefault(value, 0));
        } else if (expression instanceof LiteralStringValueExpr) {
            final String value = ((LiteralStringValueExpr) expression).getValue();
            tokensVector.put(value, 1 + tokensVector.getOrDefault(value, 0));
        } else if (expression instanceof SwitchExpr) {
            tokensVector.put("switch", 1 + tokensVector.getOrDefault("switch", 0));
            ((SwitchExpr) expression).getEntries().forEach(entry -> processSwitchEntry(entry, tokensVector));
            processExpression(((SwitchExpr) expression).getSelector(), tokensVector);
        } else if (expression instanceof VariableDeclarationExpr) {
            ((VariableDeclarationExpr) expression).getVariables().forEach(variableDeclarator -> {
                final String typeName = variableDeclarator.getType().asString();
                tokensVector.put(typeName, 1 + tokensVector.getOrDefault(typeName, 0));
                final String var = variableDeclarator.getName().asString();
                tokensVector.put(var, 1 + tokensVector.getOrDefault(var, 0));
                variableDeclarator.getInitializer().ifPresent(e -> processExpression(e, tokensVector));
            });
        } else if (expression instanceof MethodReferenceExpr) {
            tokensVector.put("::", 1 + tokensVector.getOrDefault("::", 0));
            final String methodName = ((MethodReferenceExpr) expression).getIdentifier();
            tokensVector.put(methodName, 1 + tokensVector.getOrDefault(methodName, 0));
            processExpression(((MethodReferenceExpr) expression).getScope(), tokensVector);
            ((MethodReferenceExpr) expression).getTypeArguments().ifPresent(nl -> {
                for (final Type type : nl) {
                    final String typeName = type.asString();
                    tokensVector.put(typeName, 1 + tokensVector.getOrDefault(typeName, 0));
                }
            });
        } else if (expression instanceof EnclosedExpr) {
            tokensVector.put("()", 1 + tokensVector.getOrDefault("()", 0));
            processExpression(((EnclosedExpr) expression).getInner(), tokensVector);
        } else if (expression instanceof ThisExpr) {
            tokensVector.put("this", 1 + tokensVector.getOrDefault("this", 0));
        } else if (expression instanceof NameExpr) {
            final String name = ((NameExpr) expression).getName().asString();
            tokensVector.put(name, 1 + tokensVector.getOrDefault(name, 0));
        } else if (expression instanceof CastExpr) {
            final String typeName = ((CastExpr) expression).getType().asString();
            tokensVector.put("(" + typeName + ")", 1 + tokensVector.getOrDefault("(" + typeName + ")", 0));
            processExpression(((CastExpr) expression).getExpression(), tokensVector);
        } else if (expression instanceof InstanceOfExpr) {
            final String typeName = ((InstanceOfExpr) expression).getType().asString();
            tokensVector.put("(" + typeName + ")", 1 + tokensVector.getOrDefault("(" + typeName + ")", 0));
            processExpression(((InstanceOfExpr) expression).getExpression(), tokensVector);
        } else if (expression instanceof AssignExpr) {
            final String operator = ((AssignExpr) expression).getOperator().asString();
            tokensVector.put(operator, 1 + tokensVector.getOrDefault(operator, 0));
            processExpression(((AssignExpr) expression).getValue(), tokensVector);
            processExpression(((AssignExpr) expression).getTarget(), tokensVector);
        } else if (expression instanceof MethodCallExpr) {
            tokensVector.put("();", 1 + tokensVector.getOrDefault("();", 0));
            final String methodName = ((MethodCallExpr) expression).getName().asString();
            tokensVector.put(methodName, 1 + tokensVector.getOrDefault(methodName, 0));
            ((MethodCallExpr) expression).getScope().ifPresent(e -> processExpression(e, tokensVector));
            ((MethodCallExpr) expression).getTypeArguments().ifPresent(nl -> {
                for (final Type type : nl) {
                    final String typeName = type.asString();
                    tokensVector.put(typeName, 1 + tokensVector.getOrDefault(typeName, 0));
                }
            });
            ((MethodCallExpr) expression).getArguments().forEach(e -> processExpression(e, tokensVector));
        } else if (expression instanceof ConditionalExpr) {
            tokensVector.put("?:", 1 + tokensVector.getOrDefault("?:", 0));
            processExpression(((ConditionalExpr) expression).getCondition(), tokensVector);
            processExpression(((ConditionalExpr) expression).getThenExpr(), tokensVector);
            processExpression(((ConditionalExpr) expression).getElseExpr(), tokensVector);
        } else if (expression instanceof LambdaExpr) {
            tokensVector.put("->", 1 + tokensVector.getOrDefault("->", 0));
            ((LambdaExpr) expression).getExpressionBody().ifPresent(e -> processExpression(e, tokensVector));
            processStatement(((LambdaExpr) expression).getBody(), tokensVector);
            ((LambdaExpr) expression).getParameters().forEach(param -> {
                final String typeName = param.getType().asString();
                final String paramName = param.getName().asString();
                tokensVector.put(typeName, 1 + tokensVector.getOrDefault(typeName, 0));
                tokensVector.put(paramName, 1 + tokensVector.getOrDefault(paramName, 0));
            });
        } else if (expression instanceof ArrayCreationExpr) {
            tokensVector.put("={}", 1 + tokensVector.getOrDefault("={}", 0));
            final String typeName = ((ArrayCreationExpr) expression).getElementType().asString();
            tokensVector.put(typeName, 1 + tokensVector.getOrDefault(typeName, 0));
            ((ArrayCreationExpr) expression).getInitializer()
                    .map(ArrayInitializerExpr::getValues)
                    .ifPresent(nl -> {
                        for (final Expression exp : nl) {
                            processExpression(exp, tokensVector);
                        }
                    });
        } else if (expression instanceof ClassExpr) {
            tokensVector.put(".class", 1 + tokensVector.getOrDefault(".class", 0));
            final String typeName = ((ClassExpr) expression).getType().asString();
            tokensVector.put(typeName, 1 + tokensVector.getOrDefault(typeName, 0));
        } else if (expression instanceof ArrayAccessExpr) {
            tokensVector.put("[]", 1 + tokensVector.getOrDefault("[]", 0));
            processExpression(((ArrayAccessExpr) expression).getName(), tokensVector);
            processExpression(((ArrayAccessExpr) expression).getIndex(), tokensVector);
        }
    }

    private String getMethodName(final String methodSig) {
        return methodSig.substring(0, methodSig.indexOf('('));
    }

    private List<String> getMethodParameterTypes(final String methodSig) {
        final String[] paramTypes = methodSig
                .substring(1 + methodSig.indexOf('('), methodSig.length() - 1)
                .split(",");
        return Arrays.asList(paramTypes);
    }

    private boolean matches(final String targetMethodSig,
                            final String methodSig) {
        final String targetMethodName = getMethodName(targetMethodSig);
        final String methodName = getMethodName(methodSig);
        if (!targetMethodName.endsWith(methodName)) {
            return false;
        }
        final List<String> targetMethodParameterTypes = getMethodParameterTypes(targetMethodSig);
        final List<String> methodParameterTypes = getMethodParameterTypes(methodSig);
        final int n = targetMethodParameterTypes.size();
        if (n != methodParameterTypes.size()) {
            return false;
        }
        int i = 0;
        while (i < n) {
            if (!targetMethodParameterTypes.get(i).endsWith(methodParameterTypes.get(i))) {
                return false;
            }
            i++;
        }
        return true;
    }

    private String buildMethodFullSignature(final CallableDeclaration<?> cd) {
        final StringBuilder sigBuilder = new StringBuilder();
        Node enclosingElement = cd.getParentNode().orElse(null);
        while (enclosingElement instanceof ClassOrInterfaceDeclaration) {
            sigBuilder.insert(0, ((ClassOrInterfaceDeclaration) enclosingElement).getName().asString() + ".");
            enclosingElement = enclosingElement.getParentNode().orElse(null);
        }
        String methodFullName = cd.getDeclarationAsString(false, false, false);
        methodFullName = sanitizeGenericTypeName(methodFullName);
        if (cd instanceof ConstructorDeclaration) {
            return sigBuilder.append(methodFullName.replaceAll("\\s", "")).toString();
        }
        final int indexOfFirstSpace = methodFullName.indexOf(' ');
        methodFullName = methodFullName.substring(1 + indexOfFirstSpace).replaceAll("\\s", "");
        sigBuilder.append(methodFullName);
        return sigBuilder.toString();
    }

    private String sanitizeGenericTypeName(final String genericTypeName) {
        final StringBuilder sb = new StringBuilder();
        int count = 0;
        for (final int ch : genericTypeName.toCharArray()) {
            if (ch == '<') {
                count++;
            } else if (ch == '>') {
                count--;
            } else if (count == 0) {
                sb.appendCodePoint(ch);
            }
        }
        return sb.toString();
    }
}
