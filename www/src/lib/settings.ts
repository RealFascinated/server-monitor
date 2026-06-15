export const SettingType = {
  BOOLEAN: "BOOLEAN",
  STRING: "STRING",
  INTEGER: "INTEGER",
} as const

export type SettingTypeName = (typeof SettingType)[keyof typeof SettingType]

type SettingDefinition<T extends SettingTypeName, TValue> = {
  key: string
  type: T
  defaultValue: TValue
}

export type BooleanSettingDefinition = SettingDefinition<"BOOLEAN", boolean>

export const Settings = {
  REGISTRATION_ENABLED: {
    key: "registration_enabled",
    type: SettingType.BOOLEAN,
    defaultValue: true,
  } satisfies SettingDefinition<"BOOLEAN", boolean>,
} as const

export type SettingKey = (typeof Settings)[keyof typeof Settings]["key"]
