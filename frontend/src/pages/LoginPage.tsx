import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { Link, useNavigate, useSearchParams } from "react-router-dom";
import { z } from "zod";
import { Button } from "@/components/ui/Button";
import { FieldError, Input, Label } from "@/components/ui/Input";
import { useAuth } from "@/hooks/useAuth";
import { isApiError } from "@/lib/api/errors";
import { loginAccount } from "@/lib/api/auth";
import { resolvePostAuthPath } from "@/lib/redirect";
import { persistUsername } from "@/lib/storage";

const schema = z.object({
  email: z.string().min(1, "Email is required").email("Email must be a valid email address"),
  password: z.string().min(1, "Password is required"),
});

type FormValues = z.infer<typeof schema>;

export function LoginPage() {
  const [params] = useSearchParams();
  const navigate = useNavigate();
  const { setToken } = useAuth();
  const form = useForm<FormValues>({
    resolver: zodResolver(schema),
    defaultValues: { email: "", password: "" },
  });

  async function onSubmit(values: FormValues) {
    try {
      const response = await loginAccount(values);
      setToken(response.token);
      persistUsername(null);
      navigate(resolvePostAuthPath(params.get("next")), { replace: true });
    } catch (error) {
      if (isApiError(error) && error.status === 401) {
        form.setError("root", { message: error.message });
        return;
      }
      if (isApiError(error) && error.status === 400 && error.details) {
        for (const [field, message] of Object.entries(error.details)) {
          if (field === "email" || field === "password") {
            form.setError(field, { message });
          }
        }
        if (!error.details.email && !error.details.password) {
          form.setError("root", { message: error.message });
        }
        return;
      }
      form.setError("root", {
        message: error instanceof Error ? error.message : "Sign in failed",
      });
    }
  }

  const next = params.get("next");
  const registerTo = next
    ? `/register?next=${encodeURIComponent(next)}`
    : "/register";

  return (
    <div className="mx-auto max-w-md space-y-6 pt-8">
      <div>
        <h1 className="font-display text-3xl text-zinc-50 italic">Sign in</h1>
        <p className="mt-2 text-sm text-zinc-400">
          New here?{" "}
          <Link to={registerTo} className="text-zinc-100 underline-offset-4 hover:underline">
            Create an account
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
          <Label htmlFor="password">Password</Label>
          <Input
            id="password"
            type="password"
            autoComplete="current-password"
            {...form.register("password")}
          />
          <FieldError message={form.formState.errors.password?.message} />
        </div>
        <FieldError message={form.formState.errors.root?.message} />
        <Button type="submit" className="w-full" disabled={form.formState.isSubmitting}>
          {form.formState.isSubmitting ? "Signing in…" : "Sign in"}
        </Button>
      </form>
    </div>
  );
}
