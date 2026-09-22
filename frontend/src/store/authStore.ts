import { create } from "zustand";
import { persistToken, readToken } from "@/lib/storage";

type AuthState = {
  token: string | null;
  setToken: (token: string | null) => void;
};

export const useAuthStore = create<AuthState>((set) => ({
  token: readToken(),
  setToken: (token) => {
    persistToken(token);
    set({ token });
  },
}));
