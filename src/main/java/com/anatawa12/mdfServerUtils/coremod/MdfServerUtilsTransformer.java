package com.anatawa12.mdfServerUtils.coremod;

import com.anatawa12.mdfServerUtils.Hooks;
import net.minecraft.launchwrapper.IClassTransformer;
import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AnnotationNode;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@SuppressWarnings("unused")
public class MdfServerUtilsTransformer implements IClassTransformer {
    boolean hookLoaded = false;

    @Override
    public byte[] transform(String name, String transformedName, byte[] basicClass) {
        if (!hookLoaded) {
            hookLoaded = true;
            // load class
            //noinspection ResultOfMethodCallIgnored
            Hooks.class.getClass();
        }
        if (transformedName == null) transformedName = name;

        if (basicClass == null) return null;
        if (transformedName == null) return basicClass;

        if (transformedName.equals(hooksName)) {
            readHooks(basicClass);
            return basicClass;
        }
        HooksForClass forClass = classes.get(transformedName);

        if (forClass != null) {
            ClassReader cr = new ClassReader(basicClass);
            ClassWriter cw = new ClassWriter(0);
            cr.accept(new ClassVisitor(Opcodes.ASM5, cw) {
                @Override
                public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
                    int index = desc.indexOf(')');
                    String nameAndDesc = name + desc.substring(0, index + 1);
                    List<HooksForMethod> hooks = forClass.methods.get(nameAndDesc);
                    if (hooks != null) {
                        MethodVisitor mv = super.visitMethod(access, name, desc, signature, exceptions);
                        for (HooksForMethod hook : hooks) {
                            mv = hook.makeVisitor(mv);
                        }
                        return mv;
                    }
                    return super.visitMethod(access, name, desc, signature, exceptions);
                }
            }, 0);
            return cw.toByteArray();
        }

        return basicClass;
    }

    private void readHooks(byte[] basicClass) {
        ClassReader cr = new ClassReader(basicClass);
        cr.accept(new ClassVisitor(Opcodes.ASM5) {
            @Override
            public MethodVisitor visitMethod(int access, String name, String descriptor, String signature, String[] exceptions) {
                return new MethodVisitor(Opcodes.ASM5) {
                    @Override
                    public AnnotationVisitor visitAnnotation(String desc, boolean visible) {
                        if (desc.equals(hookAtFirst.getDescriptor())) {
                            return new AnnotationNode(Opcodes.ASM5, desc) {
                                @Override
                                public void visitEnd() {
                                    super.visitEnd();

                                    Type targetOwner = null;
                                    String targetName = null;
                                    String targetNameMcp = null;
                                    List<Type> targetArgs = null;
                                    int[] realArgs = null;

                                    for (int i = 0; i < values.size(); i += 2) {
                                        switch ((String) values.get(i)) {
                                            case "owner":
                                                targetOwner = (Type) values.get(i + 1);
                                                break;
                                            case "nameMcp":
                                                targetName = (String) values.get(i + 1);
                                                break;
                                            case "name":
                                                targetNameMcp = (String) values.get(i + 1);
                                                break;
                                            case "args":
                                                //noinspection unchecked
                                                targetArgs = (List<Type>) values.get(i + 1);
                                                break;
                                            case "realArgs":
                                                realArgs = (int[]) values.get(i + 1);
                                                break;
                                        }
                                    }

                                    if (targetOwner == null) throw new IllegalArgumentException("invalid arguments");
                                    if (targetName == null) throw new IllegalArgumentException("invalid arguments");
                                    if (targetNameMcp == null) throw new IllegalArgumentException("invalid arguments");
                                    if (targetArgs == null) throw new IllegalArgumentException("invalid arguments");
                                    if (realArgs == null) throw new IllegalArgumentException("invalid arguments");

                                    String targetDesc = Type.getMethodDescriptor(Type.VOID_TYPE, targetArgs.toArray(new Type[0]));
                                    String nameAndDesc = targetName + targetDesc.substring(0, targetDesc.length() - 1);
                                    String nameAndDesc1 = targetNameMcp + targetDesc.substring(0, targetDesc.length() - 1);
                                    HooksForClass hooksForClass = getFor(targetOwner.getInternalName());
                                    HooksForMethod hook = new HooksForMethod(
                                            HooksForMethod.Kind.AddFirst,
                                            hooksInternalName,
                                            name,
                                            descriptor,
                                            realArgs
                                    );
                                    hooksForClass.add(nameAndDesc, hook);
                                    hooksForClass.add(nameAndDesc1, hook);
                                }
                            };
                        }
                        return super.visitAnnotation(desc, visible);
                    }
                };
            }
        }, ClassReader.SKIP_CODE);
    }

    // Hooks
    private static final String hooksInternalName = "com/anatawa12/mdfServerUtils/Hooks";
    private static final String hooksName = "com.anatawa12.mdfServerUtils.Hooks";

    // annotations
    private static final Type hookAtFirst = Type.getType(HookAtFirst.class);

    @SuppressWarnings("unused")
    public @interface HookAtFirst {
        Class<?> owner();

        String name();

        String nameMcp();

        Class<?>[] args();

        int[] realArgs();
    }

    /**
     * name -> HooksForMethod
     */
    private final Map<String, HooksForClass> classes = new HashMap<>();

    private HooksForClass getFor(String internalName) {
        String name = internalName.replace('/', '.');
        return classes.computeIfAbsent(name, (_1) -> new HooksForClass());
    }

    private static class HooksForClass {
        /**
         * name(arguments) -> HooksForMethod
         */
        private final Map<String, List<HooksForMethod>> methods = new HashMap<>();

        private void add(String nameAndDesc, HooksForMethod method) {
            methods.computeIfAbsent(nameAndDesc, (_1) -> new ArrayList<>())
                    .add(method);
        }

    }

    private static class HooksForMethod {
        final Kind kind;
        final String owner;
        final String name;
        final String desc;
        final int[] realArgs;

        /**
         * @param kind     hook kind
         * @param owner    hook owner
         * @param name     hook name
         * @param desc     hook descriptor
         * @param realArgs argument local variable indices
         */
        private HooksForMethod(Kind kind, String owner, String name, String desc, int[] realArgs) {
            this.kind = kind;
            this.owner = owner;
            this.name = name;
            this.desc = desc;
            this.realArgs = realArgs;
        }

        private MethodVisitor makeVisitor(MethodVisitor visitor) {
            //noinspection SwitchStatementWithTooFewBranches
            switch (kind) {
                case AddFirst:
                    return new MethodVisitor(Opcodes.ASM5, visitor) {
                        @Override
                        public void visitCode() {
                            super.visitCode();
                            Type[] args = Type.getArgumentTypes(desc);
                            for (int i = 0; i < realArgs.length; i++) {
                                super.visitVarInsn(args[i].getOpcode(Opcodes.ILOAD), realArgs[i]);
                            }
                            super.visitMethodInsn(Opcodes.INVOKESTATIC, owner, name, desc, false);
                            switch (desc.charAt(desc.length() - 1)) {
                                case 'V':
                                    break;
                                case 'J':
                                case 'D':
                                    if (desc.charAt(desc.length() - 2) == '[') {
                                        super.visitInsn(Opcodes.POP);
                                    } else {
                                        super.visitInsn(Opcodes.POP2);
                                    }
                                    break;
                                // int-based values
                                // float
                                // objects
                                default:
                                    super.visitInsn(Opcodes.POP);
                                    break;
                            }
                        }
                    };
                default:
                    throw new AssertionError();
            }
        }

        enum Kind {
            AddFirst
        }
    }
}
