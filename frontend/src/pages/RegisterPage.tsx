import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { Link, useNavigate, useSearchParams } from "react-router-dom";
import { z } from "zod";
import { Button } from "@/components/ui/Button";
import { FieldError, Input, Label } from "@/components/ui/Input";
import { useAuth } from "@/hooks/useAuth";
import { isApiError } from "@/lib/api/errors";
import { loginAccount, registerAccount } from "@/lib/api/auth";
import { resolvePostAuthPath } from "@/lib/redirect";
import { persistUsername } from "@/lib/storage";

const schema = z.object({
  email: z.string().min(1, "Email is required").email("Email must be a valid email address"),
  username: z
    .string()
    .min(1, "Username is required")
    .max(50, "Username must be at most 50 characters"),
  password: z.string().min(8, "Password must be at least 8 characters"),
});

type FormValues = z.infer<typeof schema>;

export function RegisterPage() {
  const [params] = useSearchParams();
  const navigate = useNavigate();
  const { setToken } = useAuth();
  const form = useForm<FormValues>({
    resolver: zodResolver(schema),
    defaultValues: { email: "", username: "", password: "" },
  });

  async function onSubmit(values: FormValues) {
    try {
      await registerAccount(values);
      const response = await loginAccount({
        email: values.email,
        password: values.password,
      });
      setToken(response.token);
      persistUsername(values.username);
      navigate(resolvePostAuthPath(params.get("next")), { replace: true });
    } catch (error) {
      if (isApiError(error) && error.status === 409) {
        if (error.message === "Email is already registered") {
          form.setError("email", { message: error.message });
          return;
        }
        if (error.message === "Username is already taken") {
          form.setError("username", { message: error.message });
          return;
        }
        form.setError("root", { message: error.message });
        return;
      }
      if (isApiError(error) && error.status === 400 && error.details) {
        for (const [field, message] of Object.entries(error.details)) {
          if (field === "email" || field === "username" || field === "password") {
            form.setError(field, { message });
          }
        }
        return;
      }
      if (isApiError(error) && error.status === 401) {
        form.setError("root", { message: error.message });
        return;
      }
      form.setError("root", {
        message: error instanceof Error ? error.message : "Registration failed",
      });
    }
  }

  const next = params.get("next");
  const loginTo = next ? `/login?next=${encodeURIComponent(next)}` : "/login";

  return (
    <div className="mx-auto max-w-md space-y-6 pt-8">
      <div>
        <h1 className="font-display text-3xl text-zinc-50 italic">Create account</h1>
        <p className="mt-2 text-sm text-zinc-400">
          Already have one?{" "}
          <Link to={loginTo} className="text-zinc-100 underline-offset-4 hover:underline">
            Sign in
          </Link>
        </p>
      </div>
      <form className="space-y-4" onSubmit={form.handleSubmit(onSubmit)} noValidate>
        <div>
          <Label htmlFor="email">Email</Label>
          <Input id="email" type="email" autoComplete="email" {...form.register("email")} />
          <FieldError message={form.formState.errors.email?.message} />
        </div>
        <div>
          <Label htmlFor="username">Username</Label>
          <Input
            id="username"
            autoComplete="username"
            maxLength={50}
            {...form.register("username")}
          />
          <FieldError message={form.formState.errors.username?.message} />
        </div>
        <div>
          <Label htmlFor="password">Password</Label>
          <Input
            id="password"
            type="password"
            autoComplete="new-password"
            {...form.register("password")}
          />
          <FieldError message={form.formState.errors.password?.message} />
        </div>
        <FieldError message={form.formState.errors.root?.message} />
        <Button type="submit" className="w-full" disabled={form.formState.isSubmitting}>
          {form.formState.isSubmitting ? "Creating account…" : "Create account"}
        </Button>
      </form>
    </div>
  );
}
